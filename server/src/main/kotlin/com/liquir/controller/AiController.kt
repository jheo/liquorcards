package com.liquir.controller

import com.liquir.dto.AiLookupRequest
import com.liquir.service.AiService
import com.liquir.service.ExternalDatabaseService
import com.liquir.service.ImageGenerationService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@RestController
@RequestMapping("/api/ai")
class AiController(
    private val aiService: AiService,
    private val imageGenerationService: ImageGenerationService,
    private val externalDatabaseService: ExternalDatabaseService
) {

    private val log = LoggerFactory.getLogger(AiController::class.java)
    private val executor = Executors.newCachedThreadPool()

    @PostMapping("/search")
    fun search(@RequestBody request: AiLookupRequest): ResponseEntity<Any> {
        return try {
            // Step 1: Google Search (identify + base data)
            val googleResult = aiService.searchWithGoogle(request.name)

            // Step 2: External DB search
            val collected = externalDatabaseService.collectAll(
                googleResult.searchQueries, googleResult.category
            )

            val allData = googleResult.data + collected.data
            val allSources = (googleResult.sources + collected.sources).distinct()
            val allImageUrls = (googleResult.imageUrls + collected.data.mapNotNull { it.imageUrl }).distinct()

            if (allData.isNotEmpty()) {
                val synthesizeFuture = CompletableFuture.supplyAsync({
                    aiService.synthesizeData(googleResult.canonicalName, allData)
                }, executor)

                val imageFuture = CompletableFuture.supplyAsync({
                    try {
                        imageGenerationService.generateImage(
                            googleResult.canonicalName, null, allImageUrls
                        )
                    } catch (e: Exception) {
                        log.warn("Image generation failed: {}", e.message)
                        null
                    }
                }, executor)

                var result = synthesizeFuture.join()
                result = result.copy(dataSource = "database", dataSources = allSources)

                val generatedImage = imageFuture.join()
                if (generatedImage != null) {
                    result = result.copy(imageUrl = generatedImage)
                } else if (allImageUrls.isNotEmpty()) {
                    result = result.copy(imageUrl = allImageUrls.first())
                }

                ResponseEntity.ok(result)
            } else {
                val suggestions = aiService.suggestAlternatives(request.name)
                ResponseEntity.ok(suggestions)
            }
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to (e.message ?: "AI service unavailable")))
        } catch (e: RuntimeException) {
            log.error("AI search failed for '{}'", request.name, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to (e.message ?: "AI search failed")))
        }
    }
}
