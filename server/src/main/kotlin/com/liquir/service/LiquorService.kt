package com.liquir.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.liquir.dto.*
import com.liquir.model.Liquor
import com.liquir.repository.LiquorRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class LiquorService(
    private val liquorRepository: LiquorRepository
) {
    private val mapper = jacksonObjectMapper()

    fun findAll(
        category: String?,
        status: String?,
        search: String?,
        sort: String?
    ): List<LiquorResponse> {
        val liquors = liquorRepository.findByFilters(category, status, search)

        val sorted = when (sort?.lowercase()) {
            "name" -> liquors.sortedBy { it.name.lowercase() }
            "score" -> liquors.sortedByDescending { it.score ?: 0 }
            "createdat", "created" -> liquors.sortedByDescending { it.createdAt }
            else -> liquors.sortedByDescending { it.createdAt }
        }

        return sorted.map { it.toResponse() }
    }

    fun findById(id: Long): LiquorResponse {
        val liquor = liquorRepository.findById(id)
            .orElseThrow { NoSuchElementException("Liquor not found with id: $id") }
        return liquor.toResponse()
    }

    fun create(request: CreateLiquorRequest): LiquorResponse {
        val liquor = request.toEntity()
        return liquorRepository.save(liquor).toResponse()
    }

    fun update(id: Long, request: UpdateLiquorRequest): LiquorResponse {
        val liquor = liquorRepository.findById(id)
            .orElseThrow { NoSuchElementException("Liquor not found with id: $id") }

        request.name?.let { liquor.name = it }
        request.type?.let { liquor.type = it }
        request.category?.let { liquor.category = it }
        request.abv?.let { liquor.abv = it }
        request.age?.let { liquor.age = it }
        request.score?.let { liquor.score = it }
        request.price?.let { liquor.price = it }
        request.origin?.let { liquor.origin = it }
        request.region?.let { liquor.region = it }
        request.volume?.let { liquor.volume = it }
        request.about?.let { liquor.about = it }
        request.heritage?.let { liquor.heritage = it }
        request.profile?.let { liquor.profileJson = mapper.writeValueAsString(it) }
        request.tastingNotes?.let { liquor.tastingNotesJson = mapper.writeValueAsString(it) }
        request.imageUrl?.let { liquor.imageUrl = it }
        request.suggestedImageKeyword?.let { liquor.suggestedImageKeyword = it }
        request.status?.let { liquor.status = it }
        request.nameKo?.let { liquor.nameKo = it }
        request.typeKo?.let { liquor.typeKo = it }
        request.aboutKo?.let { liquor.aboutKo = it }
        request.heritageKo?.let { liquor.heritageKo = it }
        request.tastingNotesKo?.let { liquor.tastingNotesKoJson = mapper.writeValueAsString(it) }
        liquor.updatedAt = LocalDateTime.now()

        return liquorRepository.save(liquor).toResponse()
    }

    fun delete(id: Long) {
        if (!liquorRepository.existsById(id)) {
            throw NoSuchElementException("Liquor not found with id: $id")
        }
        liquorRepository.deleteById(id)
    }
}
