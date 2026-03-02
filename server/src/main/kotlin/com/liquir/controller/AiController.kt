package com.liquir.controller

import com.liquir.dto.AiLookupRequest
import com.liquir.dto.AiLookupResponse
import com.liquir.service.AiService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/ai")
class AiController(
    private val aiService: AiService
) {

    @PostMapping("/search")
    fun search(@RequestBody request: AiLookupRequest): ResponseEntity<Any> {
        return try {
            val result = aiService.lookupLiquor(request.name, request.provider)
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
