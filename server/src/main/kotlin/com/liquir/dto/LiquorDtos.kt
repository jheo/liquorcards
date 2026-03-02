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
    val priceUsd: Double? = null,
    val priceKrw: Int? = null,
    val origin: String? = null,
    val region: String? = null,
    val volume: String? = null,
    val volumeMl: Int? = null,
    val about: String? = null,
    val heritage: String? = null,
    val profile: Map<String, Int>? = null,
    val tastingNotes: List<String>? = null,
    val tastingDetail: String? = null,
    val tastingDetailKo: String? = null,
    val pairing: List<String>? = null,
    val pairingKo: List<String>? = null,
    val imageUrl: String? = null,
    val suggestedImageKeyword: String? = null,
    val dataSource: String? = null,
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
    val priceUsd: Double? = null,
    val priceKrw: Int? = null,
    val origin: String? = null,
    val region: String? = null,
    val volume: String? = null,
    val volumeMl: Int? = null,
    val about: String? = null,
    val heritage: String? = null,
    val profile: Map<String, Int>? = null,
    val tastingNotes: List<String>? = null,
    val tastingDetail: String? = null,
    val tastingDetailKo: String? = null,
    val pairing: List<String>? = null,
    val pairingKo: List<String>? = null,
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
    val priceUsd: Double?,
    val priceKrw: Int?,
    val origin: String?,
    val region: String?,
    val volume: String?,
    val volumeMl: Int?,
    val about: String?,
    val heritage: String?,
    val profile: Map<String, Int>?,
    val tastingNotes: List<String>?,
    val tastingDetail: String?,
    val tastingDetailKo: String?,
    val pairing: List<String>?,
    val pairingKo: List<String>?,
    val imageUrl: String?,
    val suggestedImageKeyword: String?,
    val dataSource: String?,
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

/** AI returns this to normalize user input before querying external sources */
data class NormalizedQuery(
    val canonicalName: String,
    val canonicalNameKo: String? = null,
    val category: String,
    val searchQueries: List<String>,
    val confidence: Double = 1.0
)

/** Returned when insufficient data is found */
data class SuggestionResponse(
    val found: Boolean = false,
    val message: String,
    val messageKo: String? = null,
    val suggestions: List<LiquorSuggestion> = emptyList()
)

data class LiquorSuggestion(
    val name: String,
    val nameKo: String? = null,
    val reason: String,
    val reasonKo: String? = null
)

data class DisambiguationCandidate(
    val name: String,
    val nameKo: String? = null,
    val description: String? = null,
    val descriptionKo: String? = null,
    val vintage: Int? = null
)

data class AiLookupResponse(
    val name: String?,
    val type: String?,
    val category: String?,
    val abv: Double?,
    val age: String?,
    val score: Int?,
    val price: String?,
    val priceUsd: Double? = null,
    val priceKrw: Int? = null,
    val origin: String?,
    val region: String?,
    val volume: String?,
    val volumeMl: Int? = null,
    val about: String?,
    val heritage: String?,
    val profile: Map<String, Int>?,
    val tastingNotes: List<String>?,
    val tastingDetail: String? = null,
    val pairing: List<String>? = null,
    val bottleVisualDescription: String? = null,
    val suggestedImageKeyword: String?,
    val imageUrl: String? = null,
    val dataSource: String? = null,
    val dataSources: List<String>? = null,
    // Source transparency: raw collected sources + AI reasoning (not saved to DB)
    val collectedSources: List<CollectedSourceInfo>? = null,
    val synthesisReasoning: String? = null,
    @JsonAlias("synthesis_reasoning") val synthesisReasoningAlias: String? = null,
    @JsonAlias("name_ko") val nameKo: String? = null,
    @JsonAlias("type_ko") val typeKo: String? = null,
    @JsonAlias("about_ko") val aboutKo: String? = null,
    @JsonAlias("heritage_ko") val heritageKo: String? = null,
    @JsonAlias("tastingNotes_ko") val tastingNotesKo: List<String>? = null,
    @JsonAlias("tastingDetail_ko") val tastingDetailKo: String? = null,
    @JsonAlias("pairing_ko") val pairingKo: List<String>? = null
)

/** Summary of a collected data source for transparency in the preview */
data class CollectedSourceInfo(
    val source: String,
    val name: String?,
    val fieldsFound: List<String>,
    val highlights: Map<String, String?> = emptyMap(),
    /** Original text content from this source (description, tasting notes, reviews) */
    val originalTexts: Map<String, String> = emptyMap()
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
    val pairingList: List<String>? = pairingJson?.let {
        try { mapper.readValue(it) } catch (_: Exception) { null }
    }
    val pairingKoList: List<String>? = pairingKoJson?.let {
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
        priceUsd = priceUsd,
        priceKrw = priceKrw,
        origin = origin,
        region = region,
        volume = volume,
        volumeMl = volumeMl,
        about = about,
        heritage = heritage,
        profile = profileMap,
        tastingNotes = notesList,
        tastingDetail = tastingDetail,
        tastingDetailKo = tastingDetailKo,
        pairing = pairingList,
        pairingKo = pairingKoList,
        imageUrl = imageUrl,
        suggestedImageKeyword = suggestedImageKeyword,
        dataSource = dataSource,
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
        priceUsd = priceUsd,
        priceKrw = priceKrw,
        origin = origin,
        region = region,
        volume = volume,
        volumeMl = volumeMl,
        about = about,
        heritage = heritage,
        profileJson = profile?.let { mapper.writeValueAsString(it) },
        tastingNotesJson = tastingNotes?.let { mapper.writeValueAsString(it) },
        tastingDetail = tastingDetail,
        tastingDetailKo = tastingDetailKo,
        pairingJson = pairing?.let { mapper.writeValueAsString(it) },
        pairingKoJson = pairingKo?.let { mapper.writeValueAsString(it) },
        imageUrl = imageUrl,
        suggestedImageKeyword = suggestedImageKeyword,
        dataSource = dataSource,
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
