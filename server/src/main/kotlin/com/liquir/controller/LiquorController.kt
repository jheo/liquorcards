package com.liquir.controller

import com.liquir.dto.*
import com.liquir.service.AiService
import com.liquir.service.LiquorService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/liquors")
class LiquorController(
    private val liquorService: LiquorService,
    private val aiService: AiService
) {

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

    @PostMapping("/ai-lookup")
    fun aiLookup(@RequestBody request: AiLookupRequest): ResponseEntity<Any> {
        return try {
            val result = aiService.lookupLiquor(request.name, request.provider)
            ResponseEntity.ok(result)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("error" to (e.message ?: "AI service unavailable")))
        } catch (e: RuntimeException) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to (e.message ?: "AI lookup failed")))
        }
    }
}
