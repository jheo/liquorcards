package com.liquir.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.liquir.model.Liquor
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDateTime

data class CreateLiquorRequest(
    val name: String,
    val type: String? = null,
    val category: String? = null,
    val abv: Double? = null,
    val age: String? = null,
    val score: Int? = null,
    val price: String? = null,
    val origin: String? = null,
    val region: String? = null,
    val volume: String? = null,
    val about: String? = null,
    val heritage: String? = null,
    val profile: Map<String, Int>? = null,
    val tastingNotes: List<String>? = null,
    val imageUrl: String? = null,
    val suggestedImageKeyword: String? = null,
    val status: String = "active",
    val nameKo: String? = null,
    val typeKo: String? = null,
    val aboutKo: String? = null,
    val heritageKo: String? = null,
    val tastingNotesKo: List<String>? = null
)

data class UpdateLiquorRequest(
    val name: String? = null,
    val type: String? = null,
    val category: String? = null,
    val abv: Double? = null,
    val age: String? = null,
    val score: Int? = null,
    val price: String? = null,
    val origin: String? = null,
    val region: String? = null,
    val volume: String? = null,
    val about: String? = null,
    val heritage: String? = null,
    val profile: Map<String, Int>? = null,
    val tastingNotes: List<String>? = null,
    val imageUrl: String? = null,
    val suggestedImageKeyword: String? = null,
    val status: String? = null,
    val nameKo: String? = null,
    val typeKo: String? = null,
    val aboutKo: String? = null,
    val heritageKo: String? = null,
    val tastingNotesKo: List<String>? = null
)

data class LiquorResponse(
    val id: Long,
    val name: String,
    val type: String?,
    val category: String?,
    val abv: Double?,
    val age: String?,
    val score: Int?,
    val price: String?,
    val origin: String?,
    val region: String?,
    val volume: String?,
    val about: String?,
    val heritage: String?,
    val profile: Map<String, Int>?,
    val tastingNotes: List<String>?,
    val imageUrl: String?,
    val suggestedImageKeyword: String?,
    val status: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val nameKo: String?,
    val typeKo: String?,
    val aboutKo: String?,
    val heritageKo: String?,
    val tastingNotesKo: List<String>?
)

data class AiLookupRequest(
    val name: String,
    val provider: String = "claude"
)

data class AiLookupResponse(
    val name: String?,
    val type: String?,
    val category: String?,
    val abv: Double?,
    val age: String?,
    val score: Int?,
    val price: String?,
    val origin: String?,
    val region: String?,
    val volume: String?,
    val about: String?,
    val heritage: String?,
    val profile: Map<String, Int>?,
    val tastingNotes: List<String>?,
    val suggestedImageKeyword: String?,
    val imageUrl: String? = null,
    @JsonAlias("name_ko") val nameKo: String? = null,
    @JsonAlias("type_ko") val typeKo: String? = null,
    @JsonAlias("about_ko") val aboutKo: String? = null,
    @JsonAlias("heritage_ko") val heritageKo: String? = null,
    @JsonAlias("tastingNotes_ko") val tastingNotesKo: List<String>? = null
)

private val mapper = jacksonObjectMapper()

fun Liquor.toResponse(): LiquorResponse {
    val profileMap: Map<String, Int>? = profileJson?.let {
        try { mapper.readValue(it) } catch (_: Exception) { null }
    }
    val notesList: List<String>? = tastingNotesJson?.let {
        try { mapper.readValue(it) } catch (_: Exception) { null }
    }
    val notesKoList: List<String>? = tastingNotesKoJson?.let {
        try { mapper.readValue(it) } catch (_: Exception) { null }
    }
    return LiquorResponse(
        id = id!!,
        name = name,
        type = type,
        category = category,
        abv = abv,
        age = age,
        score = score,
        price = price,
        origin = origin,
        region = region,
        volume = volume,
        about = about,
        heritage = heritage,
        profile = profileMap,
        tastingNotes = notesList,
        imageUrl = imageUrl,
        suggestedImageKeyword = suggestedImageKeyword,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        nameKo = nameKo,
        typeKo = typeKo,
        aboutKo = aboutKo,
        heritageKo = heritageKo,
        tastingNotesKo = notesKoList
    )
}

fun CreateLiquorRequest.toEntity(): Liquor {
    val now = LocalDateTime.now()
    return Liquor(
        name = name,
        type = type,
        category = category,
        abv = abv,
        age = age,
        score = score,
        price = price,
        origin = origin,
        region = region,
        volume = volume,
        about = about,
        heritage = heritage,
        profileJson = profile?.let { mapper.writeValueAsString(it) },
        tastingNotesJson = tastingNotes?.let { mapper.writeValueAsString(it) },
        imageUrl = imageUrl,
        suggestedImageKeyword = suggestedImageKeyword,
        status = status,
        createdAt = now,
        updatedAt = now,
        nameKo = nameKo,
        typeKo = typeKo,
        aboutKo = aboutKo,
        heritageKo = heritageKo,
        tastingNotesKoJson = tastingNotesKo?.let { mapper.writeValueAsString(it) }
    )
}
