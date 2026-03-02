package com.liquir.controller

import com.liquir.dto.*
import com.liquir.service.AiService
import com.liquir.service.ExternalDatabaseService
import com.liquir.service.ImageGenerationService
import com.liquir.service.LiquorService
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
    private val externalDatabaseService: ExternalDatabaseService
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
     * New pipeline:
     * 1. AI normalizes user input → canonical name + search queries + category
     * 2. External DB + crawling → collect all data from multiple sources
     * 3a. Enough data → AI synthesizes into final response
     * 3b. Not enough → return suggestions
     */
    @PostMapping("/ai-lookup")
    fun aiLookup(@RequestBody request: AiLookupRequest): ResponseEntity<Any> {
        return try {
            // Step 1: AI normalizes the query
            log.info("Step 1: Normalizing query '{}'", request.name)
            val normalized = aiService.normalizeQuery(request.name, request.provider)
            log.info("Normalized: '{}' → '{}' (category: {}, confidence: {})",
                request.name, normalized.canonicalName, normalized.category, normalized.confidence)

            // Step 2: Collect data from all sources (APIs + crawling)
            log.info("Step 2: Collecting data from external sources for '{}'", normalized.canonicalName)
            val collected = externalDatabaseService.collectAll(normalized.searchQueries, normalized.category)
            log.info("Collected {} data items from sources: {}", collected.data.size, collected.sources)

            // Step 3: Decide based on collected data
            if (collected.found && collected.data.isNotEmpty()) {
                log.info("Step 3: Synthesizing + image generation in parallel ({} data items)", collected.data.size)
                val externalImageUrls = collected.data.mapNotNull { it.imageUrl }

                // Run in parallel
                val synthesizeFuture = CompletableFuture.supplyAsync({
                    aiService.synthesizeData(normalized.canonicalName, collected.data, request.provider)
                }, sseExecutor)

                val imageFuture = CompletableFuture.supplyAsync({
                    try {
                        imageGenerationService.generateImage(
                            normalized.canonicalName, null, externalImageUrls
                        )
                    } catch (e: Exception) {
                        log.warn("Image generation failed: {}", e.message)
                        null
                    }
                }, sseExecutor)

                var result = synthesizeFuture.join()
                result = result.copy(dataSource = "database", dataSources = collected.sources)

                val generatedImage = imageFuture.join()
                if (generatedImage != null) {
                    result = result.copy(imageUrl = generatedImage)
                } else if (externalImageUrls.isNotEmpty()) {
                    result = result.copy(imageUrl = externalImageUrls.first())
                }

                ResponseEntity.ok(result)
            } else {
                // Not enough data → return suggestions
                log.info("Step 3b: Insufficient data, generating suggestions for '{}'", request.name)
                val suggestions = aiService.suggestAlternatives(request.name, request.provider)
                ResponseEntity.ok(suggestions)
            }
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to (e.message ?: "AI service unavailable")))
        } catch (e: RuntimeException) {
            log.error("AI lookup failed for '{}'", request.name, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to (e.message ?: "AI lookup failed")))
        }
    }

    /**
     * SSE streaming version of ai-lookup that sends progress events.
     */
    @PostMapping("/ai-lookup-stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun aiLookupStream(@RequestBody request: AiLookupRequest): SseEmitter {
        val emitter = SseEmitter(120_000L) // 2 min timeout

        sseExecutor.execute {
            try {
                fun sendProgress(step: String, message: String, messageKo: String) {
                    try {
                        val data = mapper.writeValueAsString(mapOf(
                            "step" to step,
                            "message" to message,
                            "messageKo" to messageKo
                        ))
                        emitter.send(SseEmitter.event().name("progress").data(data))
                    } catch (e: Exception) {
                        log.debug("Failed to send SSE progress: {}", e.message)
                    }
                }

                // Step 1: Normalize
                sendProgress(
                    "normalizing",
                    "Analyzing liquor name with AI...",
                    "AI가 주류 이름을 분석하고 있습니다..."
                )

                val normalized = aiService.normalizeQuery(request.name, request.provider)

                sendProgress(
                    "normalized",
                    "${request.name} → ${normalized.canonicalName} (${normalized.category})",
                    "${request.name} → ${normalized.canonicalName} (${normalized.category})"
                )

                // Step 2: Collect from sources
                sendProgress(
                    "collecting",
                    "Searching external databases...",
                    "외부 데이터베이스 검색 중..."
                )

                val collected = externalDatabaseService.collectAll(
                    normalized.searchQueries,
                    normalized.category
                ) { sourceName, sourceNameKo ->
                    sendProgress("collecting", "Searching $sourceName...", "${sourceNameKo} 검색 중...")
                }

                if (collected.found && collected.data.isNotEmpty()) {
                    val externalImageUrls = collected.data.mapNotNull { it.imageUrl }

                    // Run AI synthesis and image generation IN PARALLEL
                    sendProgress(
                        "synthesizing",
                        "AI synthesis + image generation running in parallel...",
                        "AI 데이터 종합 + 이미지 생성을 동시에 진행하고 있습니다..."
                    )

                    val synthesizeFuture = CompletableFuture.supplyAsync({
                        aiService.synthesizeData(normalized.canonicalName, collected.data, request.provider)
                    }, sseExecutor)

                    val imageFuture = CompletableFuture.supplyAsync({
                        try {
                            imageGenerationService.generateImage(
                                normalized.canonicalName, null, externalImageUrls
                            )
                        } catch (e: Exception) {
                            log.warn("Image generation failed: {}", e.message)
                            null
                        }
                    }, sseExecutor)

                    // Wait for AI synthesis first (usually faster than image gen)
                    var result = synthesizeFuture.join()
                    result = result.copy(dataSource = "database", dataSources = collected.sources)

                    sendProgress(
                        "generating_image",
                        "Waiting for image generation...",
                        "이미지 생성을 기다리고 있습니다..."
                    )

                    // Now wait for image
                    val generatedImage = imageFuture.join()
                    if (generatedImage != null) {
                        result = result.copy(imageUrl = generatedImage)
                    } else if (externalImageUrls.isNotEmpty()) {
                        result = result.copy(imageUrl = externalImageUrls.first())
                    }

                    val resultJson = mapper.writeValueAsString(result)
                    emitter.send(SseEmitter.event().name("done").data(resultJson))
                } else {
                    sendProgress(
                        "suggesting",
                        "Generating alternative suggestions...",
                        "대체 추천 검색어를 생성하고 있습니다..."
                    )

                    val suggestions = aiService.suggestAlternatives(request.name, request.provider)
                    val suggestionsJson = mapper.writeValueAsString(suggestions)
                    emitter.send(SseEmitter.event().name("not_found").data(suggestionsJson))
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
}
