package com.liquir.controller

import com.liquir.dto.AiLookupRequest
import com.liquir.dto.AiLookupResponse
import com.liquir.service.AiService
import com.liquir.service.ImageGenerationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/ai")
class AiController(
    private val aiService: AiService,
    private val imageGenerationService: ImageGenerationService
) {

    @PostMapping("/search")
    fun search(@RequestBody request: AiLookupRequest): ResponseEntity<Any> {
        return try {
            val aiResult = aiService.lookupLiquor(request.name, request.provider)
            val keyword = aiResult.suggestedImageKeyword
            val result = if (keyword != null) {
                val imageUrl = imageGenerationService.generateImage(keyword)
                if (imageUrl != null) aiResult.copy(imageUrl = imageUrl) else aiResult
            } else aiResult
            ResponseEntity.ok(result)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to (e.message ?: "AI service unavailable")))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to (e.message ?: "AI search failed")))
        }
    }
}
