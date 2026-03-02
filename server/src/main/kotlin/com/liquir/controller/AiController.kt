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
            val normalized = aiService.normalizeQuery(request.name, request.provider)
            val collected = externalDatabaseService.collectAll(normalized.searchQueries, normalized.category)

            if (collected.found && collected.data.isNotEmpty()) {
                val externalImageUrls = collected.data.mapNotNull { it.imageUrl }

                // Run AI synthesis + image generation in parallel
                val synthesizeFuture = CompletableFuture.supplyAsync({
                    aiService.synthesizeData(normalized.canonicalName, collected.data, request.provider)
                }, executor)

                val imageFuture = CompletableFuture.supplyAsync({
                    try {
                        imageGenerationService.generateImage(
                            normalized.canonicalName, null, externalImageUrls
                        )
                    } catch (e: Exception) {
                        log.warn("Image generation failed: {}", e.message)
                        null
                    }
                }, executor)

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
                val suggestions = aiService.suggestAlternatives(request.name, request.provider)
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
