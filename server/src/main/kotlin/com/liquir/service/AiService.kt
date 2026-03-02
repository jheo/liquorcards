package com.liquir.service

import com.liquir.dto.AiLookupResponse
import com.liquir.dto.DisambiguationCandidate
import com.liquir.dto.SuggestionResponse

/**
 * Step 1 결과: Google Search grounding으로 정규화 + 기본 정보 획득
 */
data class GoogleSearchResult(
    val canonicalName: String,
    val canonicalNameKo: String?,
    val category: String,
    val searchQueries: List<String>,
    val confidence: Double,
    val data: List<ExternalLookupData>,
    val imageUrls: List<String>,
    val sources: List<String>,
    val isAmbiguous: Boolean = false,
    val candidates: List<DisambiguationCandidate> = emptyList(),
    val disambiguationType: String? = null
)

interface AiService {
    /**
     * Step 1: Gemini + Google Search grounding
     * 정규화(canonical name, category, search queries) + 기본 정보 최대한 획득
     */
    fun searchWithGoogle(userInput: String): GoogleSearchResult

    /** Step 3: 모든 데이터 병합 → 최종 종합 */
    fun synthesizeData(
        name: String,
        collectedData: List<ExternalLookupData>
    ): AiLookupResponse

    /** Step 3 (alt): 데이터 부족 시 대안 추천 */
    fun suggestAlternatives(userInput: String): SuggestionResponse
}
