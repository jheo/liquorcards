package com.liquir.controller

import com.liquir.dto.*
import com.liquir.service.AiService
import com.liquir.service.ExternalDatabaseService
import com.liquir.service.ImageGenerationService
import com.liquir.service.LiquorService
import com.liquir.service.SearchResultCache
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@RestController
@RequestMapping("/api/liquors")
class LiquorController(
    private val liquorService: LiquorService,
    private val aiService: AiService,
    private val imageGenerationService: ImageGenerationService,
    private val externalDatabaseService: ExternalDatabaseService,
    private val searchResultCache: SearchResultCache
) {

    private val log = LoggerFactory.getLogger(LiquorController::class.java)
    private val sseExecutor = Executors.newCachedThreadPool()
    private val mapper = jacksonObjectMapper()

    @GetMapping
    fun getAll(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) sort: String?
    ): ResponseEntity<List<LiquorResponse>> {
        val liquors = liquorService.findAll(category, status, search, sort)
        return ResponseEntity.ok(liquors)
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<LiquorResponse> {
        return try {
            ResponseEntity.ok(liquorService.findById(id))
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping
    fun create(@RequestBody request: CreateLiquorRequest): ResponseEntity<LiquorResponse> {
        val created = liquorService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: UpdateLiquorRequest
    ): ResponseEntity<LiquorResponse> {
        return try {
            ResponseEntity.ok(liquorService.update(id, request))
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        return try {
            liquorService.delete(id)
            ResponseEntity.noContent().build()
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<LiquorResponse> {
        val status = body["status"]
            ?: return ResponseEntity.badRequest().build()
        return try {
            ResponseEntity.ok(liquorService.update(id, UpdateLiquorRequest(status = status)))
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Pipeline:
     * 1. Gemini + Google Search grounding (identify + collect data)
     * 2. External DB search with identified name/category (enrich)
     * 3. Merge all → synthesize + image generation (parallel)
     */
    @PostMapping("/ai-lookup")
    fun aiLookup(@RequestBody request: AiLookupRequest): ResponseEntity<Any> {
        return try {
            // Step 1: Google Search (identify + base data)
            log.info("Step 1: Google Search for '{}'", request.name)
            val googleResult = aiService.searchWithGoogle(request.name)
            log.info("Identified: '{}' ({}), {} results, confidence: {}",
                googleResult.canonicalName, googleResult.category,
                googleResult.data.size, googleResult.confidence)

            // Step 2: External DB search with identified info
            log.info("Step 2: External DB search for '{}'", googleResult.canonicalName)
            val collected = externalDatabaseService.collectAll(
                googleResult.searchQueries, googleResult.category
            )

            // Merge all data
            val allData = googleResult.data + collected.data
            val allSources = (googleResult.sources + collected.sources).distinct()
            val allImageUrls = (googleResult.imageUrls + collected.data.mapNotNull { it.imageUrl }).distinct()

            log.info("Merged {} data items from sources: {}", allData.size, allSources)

            if (allData.isEmpty()) {
                val suggestions = aiService.suggestAlternatives(request.name)
                return ResponseEntity.ok(suggestions)
            }

            // Step 3: Parallel — synthesize + image generation
            log.info("Step 3: Synthesize + image generation ({} data items)", allData.size)
            val synthesizeFuture = CompletableFuture.supplyAsync({
                aiService.synthesizeData(googleResult.canonicalName, allData)
            }, sseExecutor)

            val imageFuture = CompletableFuture.supplyAsync({
                try {
                    imageGenerationService.generateImage(googleResult.canonicalName, null, allImageUrls)
                } catch (e: Exception) {
                    log.warn("Image generation failed: {}", e.message)
                    null
                }
            }, sseExecutor)

            var result = synthesizeFuture.join()
            result = result.copy(dataSource = "database", dataSources = allSources)

            val generatedImage = imageFuture.join()
            if (generatedImage != null) {
                result = result.copy(imageUrl = generatedImage)
            } else if (allImageUrls.isNotEmpty()) {
                result = result.copy(imageUrl = allImageUrls.first())
            }

            ResponseEntity.ok(result)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to (e.message ?: "AI service unavailable")))
        } catch (e: RuntimeException) {
            log.error("AI lookup failed for '{}'", request.name, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to (e.message ?: "AI lookup failed")))
        }
    }

    @GetMapping("/search-result/{resultId}")
    fun getSearchResult(@PathVariable resultId: String): ResponseEntity<AiLookupResponse> {
        val result = searchResultCache.get(resultId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result)
    }

    /**
     * SSE streaming version.
     */
    @PostMapping("/ai-lookup-stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun aiLookupStream(@RequestBody request: AiLookupRequest): SseEmitter {
        return aiLookupStreamInternal(request, skipDisambiguation = false)
    }

    private fun aiLookupStreamInternal(request: AiLookupRequest, skipDisambiguation: Boolean): SseEmitter {
        val emitter = SseEmitter(300_000L)

        sseExecutor.execute {
            try {
                val emitterLock = Any()
                fun sendProgress(step: String, message: String, messageKo: String) {
                    try {
                        val data = mapper.writeValueAsString(mapOf(
                            "step" to step,
                            "message" to message,
                            "messageKo" to messageKo
                        ))
                        synchronized(emitterLock) {
                            emitter.send(SseEmitter.event().name("progress").data(data))
                        }
                    } catch (e: Exception) {
                        log.debug("Failed to send SSE progress: {}", e.message)
                    }
                }

                // Step 1: Google Search (identify + collect)
                sendProgress(
                    "searching_google",
                    "Searching with Google + AI identification...",
                    "Google 검색 + AI 식별 중..."
                )

                val googleResult = aiService.searchWithGoogle(request.name)

                sendProgress(
                    "identified",
                    "${request.name} → ${googleResult.canonicalName} (${googleResult.category}), ${googleResult.data.size} sources found",
                    "${request.name} → ${googleResult.canonicalName} (${googleResult.category}), ${googleResult.data.size}개 소스 발견"
                )

                // Disambiguation check (Task 1) — skip if user already selected a candidate
                if (!skipDisambiguation && googleResult.isAmbiguous && googleResult.candidates.size > 1) {
                    val disambiguationJson = mapper.writeValueAsString(mapOf(
                        "candidates" to googleResult.candidates,
                        "disambiguationType" to googleResult.disambiguationType
                    ))
                    synchronized(emitterLock) {
                        emitter.send(SseEmitter.event().name("disambiguation").data(disambiguationJson))
                    }
                    emitter.complete()
                    return@execute
                }

                // Step 2: External DB search
                sendProgress(
                    "collecting",
                    "Searching external databases...",
                    "외부 데이터베이스 검색 중..."
                )

                val collected = externalDatabaseService.collectAll(
                    googleResult.searchQueries,
                    googleResult.category
                ) { sourceName, sourceNameKo ->
                    sendProgress("collecting", "Searching $sourceName...", "${sourceNameKo} 검색 중...")
                }

                // Merge all data
                val allData = googleResult.data + collected.data
                val allSources = (googleResult.sources + collected.sources).distinct()
                val allImageUrls = (googleResult.imageUrls + collected.data.mapNotNull { it.imageUrl }).distinct()

                sendProgress(
                    "collected",
                    "Found ${allData.size} data sources total",
                    "총 ${allData.size}개의 데이터 소스를 찾았습니다"
                )

                if (allData.isEmpty()) {
                    sendProgress(
                        "not_found",
                        "No data found, generating suggestions...",
                        "데이터를 찾을 수 없어 추천 항목을 생성합니다..."
                    )
                    val suggestions = aiService.suggestAlternatives(request.name)
                    val suggestionsJson = mapper.writeValueAsString(suggestions)
                    synchronized(emitterLock) {
                        emitter.send(SseEmitter.event().name("not_found").data(suggestionsJson))
                    }
                    emitter.complete()
                    return@execute
                }

                // Step 3: Parallel — synthesize + image
                sendProgress(
                    "synthesizing",
                    "AI synthesis + image generation in parallel...",
                    "AI 데이터 종합 + 이미지 생성을 동시에 진행 중..."
                )

                val synthesizeFuture = CompletableFuture.supplyAsync({
                    aiService.synthesizeData(googleResult.canonicalName, allData)
                }, sseExecutor)

                val imageFuture = CompletableFuture.supplyAsync({
                    try {
                        imageGenerationService.generateImage(
                            googleResult.canonicalName, null, allImageUrls
                        )
                    } catch (e: Exception) {
                        log.warn("Image generation failed: {}", e.message)
                        null
                    }
                }, sseExecutor)

                var result = synthesizeFuture.join()
                result = result.copy(dataSource = "database", dataSources = allSources)

                sendProgress(
                    "generating_image",
                    "Waiting for image generation...",
                    "이미지 생성을 기다리고 있습니다..."
                )

                val generatedImage = imageFuture.join()
                if (generatedImage != null) {
                    result = result.copy(imageUrl = generatedImage)
                } else if (allImageUrls.isNotEmpty()) {
                    result = result.copy(imageUrl = allImageUrls.first())
                }

                val resultId = searchResultCache.store(result)
                val doneJson = mapper.writeValueAsString(mapOf("resultId" to resultId))
                synchronized(emitterLock) {
                    emitter.send(SseEmitter.event().name("done").data(doneJson))
                }

                emitter.complete()
            } catch (e: IllegalStateException) {
                try {
                    val errorJson = mapper.writeValueAsString(mapOf("error" to (e.message ?: "AI service unavailable")))
                    emitter.send(SseEmitter.event().name("error").data(errorJson))
                    emitter.complete()
                } catch (_: Exception) {
                    emitter.completeWithError(e)
                }
            } catch (e: Exception) {
                log.error("AI lookup stream failed for '{}'", request.name, e)
                try {
                    val errorJson = mapper.writeValueAsString(mapOf("error" to (e.message ?: "AI lookup failed")))
                    emitter.send(SseEmitter.event().name("error").data(errorJson))
                    emitter.complete()
                } catch (_: Exception) {
                    emitter.completeWithError(e)
                }
            }
        }

        return emitter
    }

    /**
     * Disambiguation selection: user picks a specific product from ambiguous results.
     * Re-runs the full pipeline with the selected name, skipping disambiguation.
     */
    @PostMapping("/ai-lookup-stream/select", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun aiLookupStreamSelect(@RequestBody request: AiLookupRequest): SseEmitter {
        return aiLookupStreamInternal(request, skipDisambiguation = true)
    }
}
