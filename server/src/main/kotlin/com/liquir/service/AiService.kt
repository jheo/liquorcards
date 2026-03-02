package com.liquir.service

import com.liquir.dto.AiLookupResponse
import com.liquir.dto.NormalizedQuery
import com.liquir.dto.SuggestionResponse

interface AiService {
    /** Legacy: pure AI generation (fallback only) */
    fun lookupLiquor(name: String, provider: String = "claude"): AiLookupResponse

    /** Step 1: Normalize user input into canonical name + search queries */
    fun normalizeQuery(userInput: String, provider: String = "claude"): NormalizedQuery

    /** Step 3: Synthesize all collected data into a final response */
    fun synthesizeData(
        name: String,
        collectedData: List<ExternalLookupData>,
        provider: String = "claude"
    ): AiLookupResponse

    /** Step 3 (alt): Not enough data, suggest alternatives */
    fun suggestAlternatives(userInput: String, provider: String = "claude"): SuggestionResponse
}
