package com.liquir.service

import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.liquir.dto.AiLookupResponse
import com.liquir.dto.CollectedSourceInfo
import com.liquir.dto.DisambiguationCandidate
import com.liquir.dto.SuggestionResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@Service
class AiServiceImpl(
    @Value("\${app.ai.google-api-key:}") private val googleApiKey: String
) : AiService {

    private val log = LoggerFactory.getLogger(AiServiceImpl::class.java)
    private val mapper = jacksonObjectMapper().apply {
        // Gemini often returns JSON with raw control characters and minor syntax issues
        factory.enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
        factory.enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature())
        factory.enable(JsonReadFeature.ALLOW_MISSING_VALUES.mappedFeature())
    }
    private val restTemplate = RestTemplate(SimpleClientHttpRequestFactory().apply {
        setConnectTimeout(15_000)   // 15s connection timeout
        setReadTimeout(120_000)     // 120s read timeout (Google Search grounding can be slow for Korean queries)
    })

    // ========== Step 1: Google Search + Normalize + Collect ==========

    /**
     * Phase 1 prompt: ask Gemini to search and return PLAIN TEXT (not JSON).
     * Google Search grounding is incompatible with responseMimeType=json,
     * so we collect data as structured text first.
     */
    private val googleSearchTextPrompt = """
        You are a bilingual (English/Korean) expert on alcoholic beverages worldwide.
        The user will give you an informal or partial name of an alcoholic beverage.
        Using Google Search, identify the exact product and extract as much factual data as possible.

        The user input may be Korean name, partial name, abbreviation, misspelling, or informal reference.
        Use Google Search to find the REAL, correct product. Do NOT guess.

        Return your findings in this EXACT plain-text format (use the headers exactly as shown):

        === IDENTIFICATION ===
        Canonical Name: [exact official English product name]
        Korean Name: [Korean name or "N/A"]
        Category: [one of: whisky, wine, gin, vodka, rum, tequila, brandy, beer, liqueur, sake, soju, other]
        Confidence: [number 0.0-1.0]
        Search Queries: [comma-separated list of 3-5 search query variations]

        === PRODUCT DATA ===
        Brand: [brand name]
        ABV: [number or "N/A"]
        Volume: [e.g. "750ml" or "N/A"]
        Origin: [country]
        Region: [specific region or "N/A"]
        Price: [approximate retail price or "N/A"]
        Style: [e.g. "Single Malt Scotch Whisky" or "N/A"]
        Description: [2-4 sentences about the product from search results]

        === TASTING NOTES ===
        [Any tasting notes, reviews, or flavor descriptions found in search results]

        === IMAGE URLS ===
        [One URL per line - product image URLs found in search results]

        === SOURCE URLS ===
        [One URL per line - source URLs where data was found]

        === AMBIGUITY CHECK ===
        AMBIGUOUS: [yes/no]
        AMBIGUITY_TYPE: [vintage/expression/variant]
          - vintage: same product with different vintages/years (mostly wine)
          - expression: same brand with different expressions/ages (mostly whisky)
          - variant: completely different products
        [If yes, list up to 5 specific product variants/vintages/expressions found:]
        CANDIDATE: [English name] | [Korean name or N/A] | [brief description] | [vintage year or N/A]
        CANDIDATE: [English name] | [Korean name or N/A] | [brief description] | [vintage year or N/A]

        RULES:
        - Extract REAL data from Google Search results. Do not fabricate.
        - Include as much factual data as possible.
        - If a field is not found, write "N/A".
        - AMBIGUITY — be VERY CONSERVATIVE. Default to AMBIGUOUS: no.
          The test: "If a customer said this name in a liquor store, would the clerk immediately know which SINGLE bottle to grab?" If yes → AMBIGUOUS: no.
          * AMBIGUOUS: no when the input includes a specific age, vintage, year, or expression (e.g. "발렌타인 30년", "Macallan 18", "Château Margaux 2015").
          * AMBIGUOUS: no when there is ONE clearly dominant/flagship product for the brand (e.g. "메이커스 마크", "잭 다니엘", "노마드 아웃랜드", "헨드릭스 진", "탄커레이"). Just return the flagship.
          * AMBIGUOUS: yes ONLY when the brand has MULTIPLE equally well-known products with NO single default (e.g. "발베니" could be 12, 14, 17, 21 — all equally popular; "맥캘란" could be 12, 18, 25).
          * AMBIGUITY_TYPE "vintage" is ONLY for wines with different harvest years. NEVER for whisky, spirits, beer, or sake.
          * AMBIGUITY_TYPE "expression" is for same brand with different age statements or sub-ranges.
        - For CANDIDATE lines: the 4th field (vintage year) should be a 4-digit year (e.g. 2018) for wines ONLY, or "N/A" for everything else.
    """.trimIndent()

    /**
     * Phase 2 prompt: convert the plain-text search results into structured JSON.
     * This call uses responseMimeType=json for guaranteed valid JSON.
     */
    private val googleSearchJsonPrompt = """
        You are a data structuring assistant. Convert the provided search results text into a single JSON OBJECT (NOT an array).

        The output MUST be a JSON object starting with { and ending with }.
        It MUST contain ALL of these top-level keys:
        {
          "canonicalName": "exact official English product name",
          "canonicalNameKo": "Korean name or null",
          "category": "one of: whisky, wine, gin, vodka, rum, tequila, brandy, beer, liqueur, sake, soju, other",
          "searchQueries": ["query1", "query2", "query3"],
          "confidence": 0.9,
          "results": [
            {
              "name": "product name",
              "brand": "brand name",
              "category": "category",
              "abv": 40.0,
              "volume": "700ml",
              "origin": "country",
              "region": "region",
              "price": "price string",
              "description": "description text",
              "style": "style type",
              "source": "google_search"
            }
          ],
          "imageUrls": ["url1", "url2"],
          "sourceUrls": ["url1", "url2"]
        }

        CRITICAL RULES:
        - Output MUST be a JSON object { }, NOT an array [ ].
        - "canonicalName" is REQUIRED and must be the first key.
        - Parse the text exactly as provided. Do not add information not present in the text.
        - "N/A" values should be omitted (not included in the JSON).
        - ABV must be a number (not a string).
        - "results" must contain EXACTLY ONE item with the key factual data. Keep the description field to 2-3 sentences maximum.
        - Keep "imageUrls" to at most 3 URLs and "sourceUrls" to at most 5 URLs.
        - The ENTIRE JSON output must be COMPACT. Do not include unnecessary whitespace or verbose descriptions.
        - Return ONLY the JSON object, nothing else.
    """.trimIndent()

    override fun searchWithGoogle(userInput: String): GoogleSearchResult {
        if (googleApiKey.isBlank()) {
            throw IllegalStateException("Google API key is not configured. Set GOOGLE_API_KEY environment variable.")
        }

        return try {
            // Phase 1: Google Search grounding → plain text (with retry on timeout)
            val searchMessage = "Find detailed information about this alcoholic beverage: \"$userInput\". " +
                    "First identify the exact product, then find ABV, origin, region, price, tasting notes, reviews, and product images."

            val rawText = try {
                callGeminiWithSearch(searchMessage, googleSearchTextPrompt)
            } catch (e: Exception) {
                if (e.message?.contains("timed out", ignoreCase = true) == true ||
                    e.message?.contains("Read timed out", ignoreCase = true) == true) {
                    log.warn("Phase 1 timed out for '{}', retrying once...", userInput)
                    callGeminiWithSearch(searchMessage, googleSearchTextPrompt)
                } else throw e
            }
            log.info("Google Search raw text length for '{}': {} chars", userInput, rawText.length)
            log.debug("Google Search raw text: {}", rawText.take(500))

            // Detect ambiguity from Phase 1 text (Task 1)
            val isAmbiguous = rawText.contains(Regex("AMBIGUOUS:\\s*yes", RegexOption.IGNORE_CASE))
            val candidates = if (isAmbiguous) parseAmbiguousCandidates(rawText) else emptyList()
            val disambiguationType = if (isAmbiguous) {
                Regex("AMBIGUITY_TYPE:\\s*(vintage|expression|variant)", RegexOption.IGNORE_CASE)
                    .find(rawText)?.groupValues?.get(1)?.lowercase()
            } else null

            // Phase 2: Convert text → valid JSON using Gemini with JSON mode (with retry)
            // Truncate raw text to prevent output token overflow in Phase 2
            val truncatedText = if (rawText.length > 10000) {
                log.info("Truncating Phase 1 text from {} to 10000 chars for Phase 2", rawText.length)
                rawText.take(10000) + "\n[... truncated for brevity]"
            } else rawText
            val structureMessage = "Convert the following search results into a single JSON object. " +
                    "The JSON must start with { and contain canonicalName as the first key.\n\n$truncatedText"
            val parsed: Map<String, Any?> = try {
                val jsonText = callGemini(structureMessage, googleSearchJsonPrompt)
                parsePhase2Json(cleanJson(jsonText))
            } catch (e: Exception) {
                log.warn("Phase 2 JSON conversion failed, retrying: {}", e.message)
                val retryText = callGemini(structureMessage, googleSearchJsonPrompt)
                parsePhase2Json(cleanJson(retryText))
            }

            val canonicalName = parsed["canonicalName"] as? String ?: userInput
            val canonicalNameKo = parsed["canonicalNameKo"] as? String
            val category = parsed["category"] as? String ?: "other"
            @Suppress("UNCHECKED_CAST")
            val searchQueries = (parsed["searchQueries"] as? List<String>) ?: listOf(canonicalName)
            val confidence = (parsed["confidence"] as? Number)?.toDouble() ?: 0.5

            val results = (parsed["results"] as? List<*>)?.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                try {
                    @Suppress("UNCHECKED_CAST")
                    val extra = (map["extra"] as? Map<String, Any?>) ?: emptyMap()
                    ExternalLookupData(
                        name = map["name"] as? String,
                        brand = map["brand"] as? String,
                        category = map["category"] as? String,
                        abv = (map["abv"] as? Number)?.toDouble(),
                        volume = map["volume"] as? String,
                        origin = map["origin"] as? String,
                        region = map["region"] as? String,
                        price = map["price"] as? String,
                        description = map["description"] as? String,
                        imageUrl = map["imageUrl"] as? String,
                        style = map["style"] as? String,
                        source = (map["source"] as? String) ?: "google_search",
                        extra = extra
                    )
                } catch (e: Exception) {
                    log.warn("Failed to parse Google Search result item: {}", e.message)
                    null
                }
            } ?: emptyList()

            val imageUrls = (parsed["imageUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val sourceUrls = (parsed["sourceUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            log.info("Google Search: '{}' → '{}' ({}), {} results, {} images",
                userInput, canonicalName, category, results.size, imageUrls.size)

            // Server-side heuristic: if canonical name closely matches the first candidate,
            // the product is already identified — skip disambiguation
            val effectivelyAmbiguous = isAmbiguous && candidates.size > 1 && run {
                val first = candidates.first().name.lowercase().trim()
                val canonical = canonicalName.lowercase().trim()
                // Ambiguous only if canonical name does NOT closely match first candidate
                !(canonical == first || first.startsWith(canonical) || canonical.startsWith(first))
            }

            if (isAmbiguous && !effectivelyAmbiguous && candidates.isNotEmpty()) {
                log.info("Disambiguation skipped: canonical '{}' matches first candidate '{}'",
                    canonicalName, candidates.first().name)
            }

            GoogleSearchResult(
                canonicalName = canonicalName,
                canonicalNameKo = canonicalNameKo,
                category = category,
                searchQueries = searchQueries,
                confidence = confidence,
                data = results,
                imageUrls = imageUrls,
                sources = sourceUrls,
                isAmbiguous = effectivelyAmbiguous,
                candidates = if (effectivelyAmbiguous) candidates else emptyList(),
                disambiguationType = if (effectivelyAmbiguous) disambiguationType else null
            )
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            log.warn("Google Search grounding failed for '{}': {}", userInput, e.message)
            GoogleSearchResult(
                canonicalName = userInput,
                canonicalNameKo = null,
                category = "other",
                searchQueries = listOf(userInput),
                confidence = 0.0,
                data = emptyList(),
                imageUrls = emptyList(),
                sources = emptyList()
            )
        }
    }

    // ========== Step 3: Synthesize Data ==========

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
        - priceUsd (number or null): price in US dollars. If only KRW is known, convert at 1 USD = 1450 KRW.
        - priceKrw (integer or null): price in Korean Won. If only USD is known, convert at 1 USD = 1450 KRW.
        - origin (string): country of origin
        - region (string): specific region e.g. "Speyside", "Bordeaux"
        - volume (string): standard bottle size e.g. "750ml"
        - volumeMl (integer): volume in milliliters e.g. 750, 1000, 1750
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
        collectedData: List<ExternalLookupData>
    ): AiLookupResponse {
        val prompt = buildSynthesizePrompt(name, collectedData)
        val responseText = callGemini(prompt, synthesizePrompt)
        var result = try {
            parseResponse(responseText)
        } catch (e: Exception) {
            log.error("Synthesize parse failed for '{}': {}", name, e.message)
            throw RuntimeException("Failed to synthesize data for '$name': ${e.message}")
        }

        result = enforceCollectedFacts(result, collectedData)

        val reasoning = result.synthesisReasoning ?: result.synthesisReasoningAlias

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
        val bestAbv = data.mapNotNull { it.abv }.firstOrNull()
        val bestVolume = data.mapNotNull { it.volume }.firstOrNull()
        val bestOrigin = data.mapNotNull { it.origin }.firstOrNull()
        val bestRegion = data.mapNotNull { it.region }.firstOrNull()
        val bestPrice = data.mapNotNull { it.price }.firstOrNull()

        // Parse volumeMl from collected volume strings
        val collectedVolumeMl = data.mapNotNull { it.volume }.firstNotNullOfOrNull { parseVolumeToMl(it) }
        val finalVolumeMl = collectedVolumeMl ?: result.volumeMl ?: parseVolumeToMl(bestVolume ?: result.volume)

        // Parse priceUsd from collected price strings
        val collectedPriceUsd = data.mapNotNull { it.price }.firstNotNullOfOrNull { parsePriceUsd(it) }
        val finalPriceUsd = collectedPriceUsd ?: result.priceUsd ?: parsePriceUsd(bestPrice ?: result.price)
        val finalPriceKrw = result.priceKrw ?: finalPriceUsd?.let { (it * 1450).toInt() }

        return result.copy(
            abv = bestAbv ?: result.abv,
            volume = bestVolume ?: result.volume,
            volumeMl = finalVolumeMl,
            origin = bestOrigin ?: result.origin,
            region = bestRegion ?: result.region,
            price = bestPrice ?: result.price,
            priceUsd = finalPriceUsd,
            priceKrw = finalPriceKrw
        )
    }

    // ========== Suggest Alternatives ==========

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

    override fun suggestAlternatives(userInput: String): SuggestionResponse {
        val responseText = callGemini(
            "The user searched for '$userInput' but we couldn't find it in any external database. Suggest alternatives.",
            suggestPrompt
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

    // ========== Core AI Communication ==========

    private fun callGemini(userMessage: String, sysPrompt: String): String {
        if (googleApiKey.isBlank()) {
            throw IllegalStateException("Google API key is not configured. Set GOOGLE_API_KEY environment variable.")
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val body = mapOf(
            "contents" to listOf(
                mapOf("role" to "user", "parts" to listOf(mapOf("text" to userMessage)))
            ),
            "systemInstruction" to mapOf(
                "parts" to listOf(mapOf("text" to sysPrompt))
            ),
            "generationConfig" to mapOf(
                "temperature" to 0.2,
                "maxOutputTokens" to 65536,
                "responseMimeType" to "application/json"
            )
        )

        val request = HttpEntity(body, headers)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$googleApiKey"

        val response = restTemplate.postForEntity(url, request, Map::class.java)
        return extractGeminiText(response.body)
    }

    private fun callGeminiWithSearch(userMessage: String, sysPrompt: String): String {
        if (googleApiKey.isBlank()) {
            throw IllegalStateException("Google API key is not configured. Set GOOGLE_API_KEY environment variable.")
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val body = mapOf(
            "contents" to listOf(
                mapOf("role" to "user", "parts" to listOf(mapOf("text" to userMessage)))
            ),
            "systemInstruction" to mapOf(
                "parts" to listOf(mapOf("text" to sysPrompt))
            ),
            "tools" to listOf(mapOf("googleSearch" to emptyMap<String, Any>())),
            "generationConfig" to mapOf(
                "temperature" to 0.2,
                "maxOutputTokens" to 65536
            )
        )

        val request = HttpEntity(body, headers)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$googleApiKey"

        val response = restTemplate.postForEntity(url, request, Map::class.java)
        return extractGeminiText(response.body)
    }

    private fun extractGeminiText(body: Map<*, *>?): String {
        val candidates = body?.get("candidates") as? List<*>
            ?: throw RuntimeException("Empty response from Gemini API")
        val firstCandidate = candidates.firstOrNull() as? Map<*, *>
            ?: throw RuntimeException("No candidates in Gemini response")
        val content = firstCandidate["content"] as? Map<*, *>
            ?: throw RuntimeException("No content in Gemini candidate")
        val parts = content["parts"] as? List<*>
            ?: throw RuntimeException("No parts in Gemini content")

        return parts.mapNotNull { part ->
            (part as? Map<*, *>)?.get("text") as? String
        }.joinToString("")
            .ifBlank { throw RuntimeException("No text content in Gemini response") }
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

        // Fix raw newlines/tabs inside JSON string values only
        cleaned = fixRawNewlinesInJson(cleaned)

        val objStart = cleaned.indexOf('{')
        val objEnd = cleaned.lastIndexOf('}')
        val arrStart = cleaned.indexOf('[')
        val arrEnd = cleaned.lastIndexOf(']')

        val useObj = objStart >= 0 && objEnd > objStart
        val useArr = arrStart >= 0 && arrEnd > arrStart

        val startPos: Int
        val endPos: Int
        if (useObj && useArr) {
            if (objStart <= arrStart) {
                startPos = objStart; endPos = objEnd
            } else {
                startPos = arrStart; endPos = arrEnd
            }
        } else if (useObj) {
            startPos = objStart; endPos = objEnd
        } else if (useArr) {
            startPos = arrStart; endPos = arrEnd
        } else {
            return cleaned
        }

        if (startPos > 0) {
            cleaned = cleaned.substring(startPos, endPos + 1)
        }

        return cleaned
    }

    /**
     * Fix raw newlines inside JSON string values.
     * Walks character by character, tracking whether we're inside a quoted string.
     */
    private fun fixRawNewlinesInJson(json: String): String {
        val sb = StringBuilder(json.length)
        var inString = false
        var i = 0
        while (i < json.length) {
            val c = json[i]
            if (c == '\\' && inString && i + 1 < json.length) {
                // Already-escaped character — pass through both chars
                sb.append(c)
                sb.append(json[i + 1])
                i += 2
                continue
            }
            if (c == '"') {
                inString = !inString
                sb.append(c)
            } else if (inString && c == '\n') {
                sb.append("\\n")
            } else if (inString && c == '\t') {
                sb.append("\\t")
            } else {
                sb.append(c)
            }
            i++
        }
        return sb.toString()
    }

    // ========== Phase 2 JSON Array Recovery (Task 4) ==========

    /**
     * Parse Phase 2 JSON, handling cases where Gemini returns an array instead of an object.
     * - If it's already an object → return as-is
     * - If it's an array of Maps → merge them into a single Map
     * - If it's an array of Strings → use first item as canonicalName hint
     */
    internal fun parsePhase2Json(json: String): Map<String, Any?> {
        val trimmed = json.trim()
        if (trimmed.startsWith("{")) {
            return mapper.readValue(trimmed)
        }
        if (trimmed.startsWith("[")) {
            val arr: List<Any?> = mapper.readValue(trimmed)
            if (arr.isEmpty()) throw RuntimeException("Phase 2 returned empty array")

            // Array of Maps → merge
            if (arr.first() is Map<*, *>) {
                val merged = mutableMapOf<String, Any?>()
                arr.filterIsInstance<Map<*, *>>().forEach { map ->
                    map.forEach { (k, v) ->
                        if (k is String && v != null && !merged.containsKey(k)) {
                            merged[k] = v
                        }
                    }
                }
                if (merged.isNotEmpty()) return merged
            }

            // Array of Strings → treat first as canonicalName
            if (arr.first() is String) {
                return mapOf(
                    "canonicalName" to arr.first(),
                    "category" to "other",
                    "searchQueries" to arr.filterIsInstance<String>(),
                    "confidence" to 0.5,
                    "results" to emptyList<Any>(),
                    "imageUrls" to emptyList<Any>(),
                    "sourceUrls" to emptyList<Any>()
                )
            }

            throw RuntimeException("Phase 2 returned unexpected array content")
        }
        return mapper.readValue(trimmed)
    }

    // ========== Ambiguity Detection (Task 1) ==========

    private fun parseAmbiguousCandidates(rawText: String): List<DisambiguationCandidate> {
        val candidates = mutableListOf<DisambiguationCandidate>()
        val pattern = Regex("CANDIDATE:\\s*(.+?)\\s*\\|\\s*(.+?)\\s*\\|\\s*(.+?)(?:\\s*\\|\\s*(.+))?$")
        for (line in rawText.lines()) {
            val match = pattern.find(line.trim()) ?: continue
            val name = match.groupValues[1].trim()
            val nameKo = match.groupValues[2].trim()
            val desc = match.groupValues[3].trim()
            val vintageStr = match.groupValues.getOrNull(4)?.trim()
            val vintage = vintageStr?.takeIf { it != "N/A" && it.isNotBlank() }?.toIntOrNull()
            candidates.add(DisambiguationCandidate(
                name = name,
                nameKo = nameKo.takeIf { it != "N/A" },
                description = desc,
                descriptionKo = null,
                vintage = vintage
            ))
        }
        return candidates
    }

    // ========== Volume / Price Parsing Helpers (Task 2, 8) ==========

    internal fun parseVolumeToMl(volumeStr: String?): Int? {
        if (volumeStr.isNullOrBlank()) return null
        val s = volumeStr.trim().lowercase()
        // "750ml" or "750 ml"
        Regex("(\\d+)\\s*ml").find(s)?.let {
            return it.groupValues[1].toIntOrNull()
        }
        // "1L", "1.75L", "1.75 L"
        Regex("([\\d.]+)\\s*l(?:iter|itre)?s?").find(s)?.let {
            val liters = it.groupValues[1].toDoubleOrNull() ?: return null
            return (liters * 1000).toInt()
        }
        // "75cl"
        Regex("(\\d+)\\s*cl").find(s)?.let {
            val cl = it.groupValues[1].toIntOrNull() ?: return null
            return cl * 10
        }
        return null
    }

    internal fun parsePriceUsd(priceStr: String?): Double? {
        if (priceStr.isNullOrBlank()) return null
        val s = priceStr.trim()
        // "$65", "$65.99", "US$65", "USD 65"
        Regex("\\$\\s*([\\d,]+\\.?\\d*)").find(s)?.let {
            return it.groupValues[1].replace(",", "").toDoubleOrNull()
        }
        Regex("USD\\s*([\\d,]+\\.?\\d*)", RegexOption.IGNORE_CASE).find(s)?.let {
            return it.groupValues[1].replace(",", "").toDoubleOrNull()
        }
        // "₩45,000" or "45000원" → convert to USD
        Regex("[₩￦]\\s*([\\d,]+)").find(s)?.let {
            val krw = it.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return krw / 1450.0
        }
        Regex("([\\d,]+)\\s*원").find(s)?.let {
            val krw = it.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            return krw / 1450.0
        }
        // Plain number → assume USD
        Regex("^[\\d,]+\\.?\\d*$").find(s)?.let {
            return it.value.replace(",", "").toDoubleOrNull()
        }
        return null
    }

}
