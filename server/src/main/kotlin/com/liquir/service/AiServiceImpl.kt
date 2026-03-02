package com.liquir.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.liquir.dto.AiLookupResponse
import com.liquir.dto.CollectedSourceInfo
import com.liquir.dto.NormalizedQuery
import com.liquir.dto.SuggestionResponse
import com.liquir.dto.LiquorSuggestion
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class AiServiceImpl(
    @Value("\${app.ai.anthropic-api-key:}") private val claudeApiKey: String,
    @Value("\${app.ai.openai-api-key:}") private val openaiApiKey: String
) : AiService {

    private val log = LoggerFactory.getLogger(AiServiceImpl::class.java)
    private val mapper = jacksonObjectMapper()
    private val restTemplate = RestTemplate()

    // ========== Step 1: Normalize Query ==========

    private val normalizePrompt = """
        You are an expert on alcoholic beverages worldwide. Your task is to identify the exact canonical name of a liquor from the user's input.

        The user may input:
        - Korean name (e.g., "발베니 12년 더블우드")
        - Partial name (e.g., "macallan 12")
        - Misspelled name (e.g., "glenfidich")
        - Informal reference (e.g., "산토리 가쿠빈")

        Return a JSON object with:
        - canonicalName (string): The exact official English name (e.g., "Balvenie 12 Year Old DoubleWood")
        - canonicalNameKo (string): Korean name (e.g., "발베니 12년 더블우드")
        - category (string): one of "whisky", "wine", "gin", "vodka", "rum", "tequila", "brandy", "beer", "liqueur", "sake", "soju", "other"
        - searchQueries (array of strings): 3-5 search query variations to use when searching databases. Include:
          * The exact canonical English name
          * Brand + product shorthand (e.g., "Balvenie DoubleWood 12")
          * Just the brand name (e.g., "Balvenie")
          * Any common alternative names or spellings
        - confidence (number 0-1): How confident you are that you correctly identified the product. 1.0 = certain, 0.5 = unsure

        Return ONLY valid JSON, no markdown, no explanation.
    """.trimIndent()

    override fun normalizeQuery(userInput: String, provider: String): NormalizedQuery {
        val responseText = callAi("Identify this alcoholic beverage: $userInput", normalizePrompt, provider)
        val cleaned = cleanJson(responseText)
        return try {
            mapper.readValue<NormalizedQuery>(cleaned)
        } catch (e: Exception) {
            log.warn("Failed to parse normalized query, using input as-is: {}", e.message)
            NormalizedQuery(
                canonicalName = userInput,
                category = "other",
                searchQueries = listOf(userInput),
                confidence = 0.5
            )
        }
    }

    // ========== Step 3a: Synthesize Data ==========

    private val synthesizePrompt = """
        You are a bilingual (English/Korean) liquor expert. You are given DATA COLLECTED FROM MULTIPLE EXTERNAL SOURCES about an alcoholic beverage.

        Your job is to SYNTHESIZE this data into a single, accurate, comprehensive result:
        1. EVALUATE the reliability of each data source. Prefer factual data (ABV, volume, origin) from known databases.
        2. When sources conflict, prefer the most commonly reported value.
        3. FILL IN missing fields using your expert knowledge ONLY if you are confident.
        4. TRANSLATE relevant fields to Korean.
        5. Generate a detailed bottle visual description for image generation.

        CRITICAL RULES:
        - You MUST ALWAYS return valid JSON. NEVER return explanations, apologies, or comments — ONLY JSON.
        - If some source data doesn't match the requested product, IGNORE that source and use the remaining sources + your knowledge.
        - Even if ALL source data is irrelevant, still return valid JSON using your expert knowledge for the requested product.
        - For factual fields (ABV, volume, origin, price): use values from external sources when they match the product. Do NOT invent these.
        - For description fields (about, heritage, tasting notes, tasting detail, pairing):
          PRIMARILY USE text from external sources (descriptions, reviews, tasting notes) when they match.
          Rewrite and enrich them, but the core content should come from external data.
          Only supplement with your knowledge when external data is insufficient or doesn't match.
        - For profile scores: base them on tasting notes and descriptions from external sources.
        - Every field in the output must be filled.

        Return a JSON object with exactly these fields:

        English fields:
        - name (string): the full official name
        - type (string): e.g. "Single Malt Scotch Whisky", "London Dry Gin"
        - category (string): one of "whisky", "wine", "gin", "vodka", "rum", "tequila", "brandy", "beer", "liqueur", "sake", "soju", "other"
        - abv (number): alcohol by volume percentage
        - age (string or null): e.g. "12 Years", "NAS"
        - score (integer): quality score out of 100
        - price (string): approximate retail price e.g. "${'$'}65"
        - origin (string): country of origin
        - region (string): specific region e.g. "Speyside", "Bordeaux"
        - volume (string): standard bottle size e.g. "750ml"
        - about (string): 2-3 sentences describing the liquor in English
        - heritage (string): 2-3 sentences about origin and history in English
        - profile (object): category-specific scores from 0-100:
          * For whisky: sweetness, body, richness, smokiness, finish, complexity
          * For wine: sweetness, acidity, tannin, body, fruitiness, complexity
          * For gin: juniper, citrus, floral, herbal, spice, complexity
          * For beer: hoppy, malty, body, bitterness, sweetness, complexity
          * For sake: sweetness, umami, acidity, body, fragrance, complexity
          * For other categories: body, sweetness, complexity, finish, aroma, smoothness
        - tastingNotes (array of strings): 4-6 tasting note keywords in English
        - tastingDetail (string): 3-5 sentences. Nose, Palate, Finish in flowing prose.
        - pairing (array of strings): 4-6 food pairing recommendations in English
        - bottleVisualDescription (string): Extremely detailed visual description of the bottle.
        - suggestedImageKeyword (string): a keyword suitable for searching a stock image

        Korean translation fields (REQUIRED):
        - name_ko, type_ko, about_ko, heritage_ko, tastingNotes_ko, tastingDetail_ko, pairing_ko

        Synthesis reasoning (REQUIRED):
        - synthesis_reasoning (string): 5-8 sentences in Korean explaining how you synthesized the data.
          MUST include ALL of the following:
          1. Which sources provided which factual data (ABV, origin, price etc.) and why you chose those values
          2. How you derived the tasting notes and tasting detail — which source descriptions/reviews you based them on
          3. How you determined each profile score — explain the reasoning for each score (e.g. "sweetness 78: whisky.com의 테이스팅 노트에서 꿀, 바닐라, 캐러멜이 언급되어 높은 점수 부여")
          4. Any conflicts between sources and how you resolved them
          5. Which fields you supplemented with your own knowledge due to lack of external data

        Return ONLY valid JSON, no markdown, no explanation, no code blocks.
    """.trimIndent()

    override fun synthesizeData(
        name: String,
        collectedData: List<ExternalLookupData>,
        provider: String
    ): AiLookupResponse {
        val prompt = buildSynthesizePrompt(name, collectedData)
        val responseText = callAi(prompt, synthesizePrompt, provider)
        var result = try {
            parseResponse(responseText)
        } catch (e: Exception) {
            log.warn("Synthesize parse failed, falling back to direct AI lookup for '{}': {}", name, e.message)
            lookupLiquor(name, provider)
        }

        // Enforce factual data from the most reliable source
        result = enforceCollectedFacts(result, collectedData)

        // Use synthesisReasoningAlias if synthesisReasoning is null (handles both JSON key variants)
        val reasoning = result.synthesisReasoning ?: result.synthesisReasoningAlias

        // Build source transparency info
        val sourceInfos = collectedData.map { data ->
            val fields = mutableListOf<String>()
            if (data.name != null) fields.add("name")
            if (data.brand != null) fields.add("brand")
            if (data.abv != null) fields.add("abv")
            if (data.volume != null) fields.add("volume")
            if (data.origin != null) fields.add("origin")
            if (data.region != null) fields.add("region")
            if (data.price != null) fields.add("price")
            if (data.description != null) fields.add("description")
            if (data.imageUrl != null) fields.add("image")
            if (data.style != null) fields.add("style")

            val highlights = mutableMapOf<String, String?>()
            data.name?.let { highlights["name"] = it }
            data.abv?.let { highlights["abv"] = "${it}%" }
            data.origin?.let { highlights["origin"] = it }
            data.price?.let { highlights["price"] = it }
            data.volume?.let { highlights["volume"] = it }

            // Collect original text content for preview transparency
            val originalTexts = mutableMapOf<String, String>()
            data.description?.let { originalTexts["description"] = it }
            data.extra["nose"]?.toString()?.let { originalTexts["nose"] = it }
            data.extra["palate"]?.toString()?.let { originalTexts["palate"] = it }
            data.extra["finish"]?.toString()?.let { originalTexts["finish"] = it }
            data.extra["flavor_notes"]?.toString()?.let { originalTexts["flavor_notes"] = it }
            data.extra["top_review"]?.toString()?.let { originalTexts["review"] = it }
            data.extra["tasting_note"]?.toString()?.let { originalTexts["tasting_note"] = it }
            data.extra["flavor_profile"]?.toString()?.let { originalTexts["flavor_profile"] = it }

            CollectedSourceInfo(
                source = data.source,
                name = data.name ?: data.brand,
                fieldsFound = fields,
                highlights = highlights,
                originalTexts = originalTexts
            )
        }

        return result.copy(
            collectedSources = sourceInfos,
            synthesisReasoning = reasoning
        )
    }

    private fun buildSynthesizePrompt(name: String, data: List<ExternalLookupData>): String {
        val sb = StringBuilder()
        sb.appendLine("Synthesize the following collected data for: $name")
        sb.appendLine()

        data.forEachIndexed { index, d ->
            sb.appendLine("=== SOURCE ${index + 1}: ${d.source.uppercase()} ===")
            d.name?.let { sb.appendLine("Name: $it") }
            d.brand?.let { sb.appendLine("Brand/Maker: $it") }
            d.category?.let { sb.appendLine("Category: $it") }
            d.abv?.let { sb.appendLine("ABV: $it%") }
            d.volume?.let { sb.appendLine("Volume: $it") }
            d.origin?.let { sb.appendLine("Origin: $it") }
            d.region?.let { sb.appendLine("Region: $it") }
            d.price?.let { sb.appendLine("Price: $it") }
            d.description?.let { sb.appendLine("Description: $it") }
            d.ingredients?.let { sb.appendLine("Ingredients: $it") }
            d.style?.let { sb.appendLine("Style/Type: $it") }
            d.extra.forEach { (key, value) ->
                if (value != null && key != "categories_raw" && key != "product_url") {
                    sb.appendLine("$key: $value")
                }
            }
            sb.appendLine()
        }

        sb.appendLine("Synthesize ALL the above into a single comprehensive JSON response. Prioritize verified factual data.")
        return sb.toString()
    }

    private fun enforceCollectedFacts(result: AiLookupResponse, data: List<ExternalLookupData>): AiLookupResponse {
        // Find the best ABV, volume, origin from collected data
        val bestAbv = data.mapNotNull { it.abv }.firstOrNull()
        val bestVolume = data.mapNotNull { it.volume }.firstOrNull()
        val bestOrigin = data.mapNotNull { it.origin }.firstOrNull()
        val bestRegion = data.mapNotNull { it.region }.firstOrNull()
        val bestPrice = data.mapNotNull { it.price }.firstOrNull()

        return result.copy(
            abv = bestAbv ?: result.abv,
            volume = bestVolume ?: result.volume,
            origin = bestOrigin ?: result.origin,
            region = bestRegion ?: result.region,
            price = bestPrice ?: result.price
        )
    }

    // ========== Step 3b: Suggest Alternatives ==========

    private val suggestPrompt = """
        You are a bilingual (English/Korean) liquor expert. The user searched for an alcoholic beverage but we could not find reliable data from external databases.

        Provide helpful suggestions. Consider:
        1. The user may have misspelled the name
        2. The user may be thinking of a different product
        3. Similar products that the user might be interested in
        4. The correct/canonical name if the user's input was informal

        Return a JSON object with:
        - found (boolean): always false
        - message (string): A helpful message in English explaining what happened
        - messageKo (string): Same message in Korean
        - suggestions (array of objects, 3-5 items): Each with:
          - name (string): Official English name of the suggested product
          - nameKo (string): Korean name
          - reason (string): Why this is suggested (English)
          - reasonKo (string): Why this is suggested (Korean)

        Return ONLY valid JSON, no markdown, no explanation.
    """.trimIndent()

    override fun suggestAlternatives(userInput: String, provider: String): SuggestionResponse {
        val responseText = callAi(
            "The user searched for '$userInput' but we couldn't find it in any external database. Suggest alternatives.",
            suggestPrompt,
            provider
        )
        val cleaned = cleanJson(responseText)
        return try {
            mapper.readValue<SuggestionResponse>(cleaned)
        } catch (e: Exception) {
            log.warn("Failed to parse suggestions: {}", e.message)
            SuggestionResponse(
                found = false,
                message = "Could not find '$userInput'. Please try a different search term.",
                messageKo = "'$userInput'을(를) 찾을 수 없습니다. 다른 검색어를 시도해주세요."
            )
        }
    }

    // ========== Legacy: Pure AI Lookup ==========

    private val systemPrompt = """
        You are a bilingual (English/Korean) liquor expert with comprehensive knowledge about alcoholic beverages worldwide.
        When given a liquor name, provide the most accurate information possible.
        Use real, verified data — do NOT fabricate or guess.

        IMPORTANT: You MUST return ALL fields listed below, including ALL Korean translation fields (_ko). Every field is required.

        Return a JSON object with exactly these fields:

        English fields:
        - name (string): the full official name
        - type (string): e.g. "Single Malt Scotch Whisky"
        - category (string): one of "whisky", "wine", "gin", "vodka", "rum", "tequila", "brandy", "beer", "liqueur", "sake", "soju", "other"
        - abv (number): alcohol by volume percentage
        - age (string or null): e.g. "12 Years", "NAS"
        - score (integer): quality score out of 100
        - price (string): approximate retail price e.g. "${'$'}65"
        - origin (string): country of origin
        - region (string): specific region
        - volume (string): standard bottle size e.g. "750ml"
        - about (string): 2-3 sentences
        - heritage (string): 2-3 sentences about origin and history
        - profile (object): category-specific scores from 0-100
        - tastingNotes (array of strings): 4-6 keywords
        - tastingDetail (string): 3-5 sentences. Nose, Palate, Finish.
        - pairing (array of strings): 4-6 food pairings
        - bottleVisualDescription (string): Detailed visual description of the bottle
        - suggestedImageKeyword (string): a keyword for image search

        Korean translation fields (REQUIRED):
        - name_ko, type_ko, about_ko, heritage_ko, tastingNotes_ko, tastingDetail_ko, pairing_ko

        Return ONLY valid JSON, no markdown, no explanation, no code blocks.
    """.trimIndent()

    override fun lookupLiquor(name: String, provider: String): AiLookupResponse {
        val responseText = callAi("Tell me about: $name", systemPrompt, provider)
        return parseResponse(responseText)
    }

    // ========== Core AI Communication ==========

    private fun callAi(userMessage: String, sysPrompt: String, provider: String): String {
        return when (provider.lowercase()) {
            "openai" -> callOpenAi(userMessage, sysPrompt)
            else -> callClaude(userMessage, sysPrompt)
        }
    }

    private fun callClaude(userMessage: String, sysPrompt: String): String {
        if (claudeApiKey.isBlank()) {
            throw IllegalStateException("Claude API key is not configured. Set ANTHROPIC_API_KEY environment variable.")
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("x-api-key", claudeApiKey)
            set("anthropic-version", "2023-06-01")
        }

        val body = mapOf(
            "model" to "claude-haiku-4-5-20251001",
            "max_tokens" to 4096,
            "system" to sysPrompt,
            "messages" to listOf(
                mapOf("role" to "user", "content" to userMessage)
            )
        )

        val request = HttpEntity(body, headers)
        val response = restTemplate.postForEntity(
            "https://api.anthropic.com/v1/messages",
            request,
            Map::class.java
        )

        val content = response.body?.get("content") as? List<*>
            ?: throw RuntimeException("Empty response from Claude API")
        val textBlock = content.firstOrNull() as? Map<*, *>
            ?: throw RuntimeException("No content block in Claude response")
        return textBlock["text"] as? String
            ?: throw RuntimeException("No text in Claude response content block")
    }

    private fun callOpenAi(userMessage: String, sysPrompt: String): String {
        if (openaiApiKey.isBlank()) {
            throw IllegalStateException("OpenAI API key is not configured. Set OPENAI_API_KEY environment variable.")
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(openaiApiKey)
        }

        val body = mapOf(
            "model" to "gpt-4o",
            "messages" to listOf(
                mapOf("role" to "system", "content" to sysPrompt),
                mapOf("role" to "user", "content" to userMessage)
            ),
            "max_tokens" to 4096
        )

        val request = HttpEntity(body, headers)
        val response = restTemplate.postForEntity(
            "https://api.openai.com/v1/chat/completions",
            request,
            Map::class.java
        )

        val choices = response.body?.get("choices") as? List<*>
            ?: throw RuntimeException("Empty response from OpenAI API")
        val choice = choices.firstOrNull() as? Map<*, *>
            ?: throw RuntimeException("No choices in OpenAI response")
        val message = choice["message"] as? Map<*, *>
            ?: throw RuntimeException("No message in OpenAI choice")
        return message["content"] as? String
            ?: throw RuntimeException("No content in OpenAI message")
    }

    private fun parseResponse(text: String): AiLookupResponse {
        val cleaned = cleanJson(text)
        return try {
            mapper.readValue<AiLookupResponse>(cleaned)
        } catch (e: Exception) {
            log.error("Failed to parse AI response: $cleaned", e)
            throw RuntimeException("Failed to parse AI response as JSON: ${e.message}")
        }
    }

    private fun cleanJson(text: String): String {
        var cleaned = text
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()

        // If AI returned text before JSON, try to extract the JSON object
        val jsonStart = cleaned.indexOf('{')
        val jsonEnd = cleaned.lastIndexOf('}')
        if (jsonStart > 0 && jsonEnd > jsonStart) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1)
        }

        return cleaned
    }
}
