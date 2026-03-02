package com.liquir.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.liquir.dto.AiLookupResponse
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

    private val systemPrompt = """
        You are a bilingual (English/Korean) liquor expert with comprehensive knowledge about alcoholic beverages worldwide.
        When given a liquor name, provide the most accurate information possible.
        Use real, verified data — do NOT fabricate or guess.

        IMPORTANT: You MUST return ALL fields listed below, including ALL Korean translation fields (_ko). Every field is required.

        Return a JSON object with exactly these fields:

        English fields:
        - name (string): the full official name
        - type (string): e.g. "Single Malt Scotch Whisky", "London Dry Gin"
        - category (string): one of "whisky", "wine", "gin", "vodka", "rum", "tequila", "brandy", "beer", "liqueur", "other"
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
          * For other categories: body, sweetness, complexity, finish, aroma, smoothness
        - tastingNotes (array of strings): 4-6 tasting note keywords in English
        - suggestedImageKeyword (string): a keyword suitable for searching a stock image

        Korean translation fields (REQUIRED — do NOT omit these):
        - name_ko (string): Korean name, e.g. "야마자키 12년"
        - type_ko (string): Korean type, e.g. "싱글 몰트 재패니즈 위스키"
        - about_ko (string): 2-3 sentences describing the liquor in Korean
        - heritage_ko (string): 2-3 sentences about origin and history in Korean
        - tastingNotes_ko (array of strings): 4-6 tasting note keywords in Korean

        Example output structure:
        {
          "name": "Yamazaki 12 Year Old",
          "type": "Single Malt Japanese Whisky",
          "category": "whisky",
          "abv": 43,
          "age": "12 Years",
          "score": 90,
          "price": "${'$'}150",
          "origin": "Japan",
          "region": "Osaka",
          "volume": "750ml",
          "about": "Yamazaki 12 is a flagship single malt...",
          "heritage": "Suntory founded the Yamazaki distillery in 1923...",
          "profile": {"sweetness": 75, "body": 70, "richness": 80, "smokiness": 15, "finish": 75, "complexity": 80},
          "tastingNotes": ["honey", "dried fruit", "oak", "vanilla", "cinnamon", "peach"],
          "suggestedImageKeyword": "yamazaki whisky bottle",
          "name_ko": "야마자키 12년",
          "type_ko": "싱글 몰트 재패니즈 위스키",
          "about_ko": "야마자키 12년은 산토리의 대표적인 싱글 몰트 위스키입니다...",
          "heritage_ko": "산토리는 1923년 야마자키 증류소를 설립했습니다...",
          "tastingNotes_ko": ["꿀", "건과일", "오크", "바닐라", "시나몬", "복숭아"]
        }

        Return ONLY valid JSON, no markdown, no explanation, no code blocks.
    """.trimIndent()

    override fun lookupLiquor(name: String, provider: String): AiLookupResponse {
        val responseText = when (provider.lowercase()) {
            "openai" -> callOpenAi(name)
            else -> callClaude(name)
        }

        return parseResponse(responseText)
    }

    private fun callClaude(name: String): String {
        if (claudeApiKey.isBlank()) {
            throw IllegalStateException("Claude API key is not configured. Set ANTHROPIC_API_KEY environment variable.")
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("x-api-key", claudeApiKey)
            set("anthropic-version", "2023-06-01")
        }

        val body = mapOf(
            "model" to "claude-sonnet-4-20250514",
            "max_tokens" to 4096,
            "system" to systemPrompt,
            "messages" to listOf(
                mapOf("role" to "user", "content" to "Tell me about: $name")
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

    private fun callOpenAi(name: String): String {
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
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to "Tell me about: $name")
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
        val cleanedText = text
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()

        return try {
            mapper.readValue<AiLookupResponse>(cleanedText)
        } catch (e: Exception) {
            log.error("Failed to parse AI response: $cleanedText", e)
            throw RuntimeException("Failed to parse AI response as JSON: ${e.message}")
        }
    }
}
