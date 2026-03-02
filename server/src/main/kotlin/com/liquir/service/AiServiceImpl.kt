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
        You are a liquor expert. Given a liquor name, return detailed information as JSON with these fields:
        - name (string): the full official name
        - type (string): e.g. "Single Malt Scotch Whisky", "London Dry Gin", "Cabernet Sauvignon"
        - category (string): one of "whisky", "wine", "gin", "vodka", "rum", "tequila", "brandy", "beer", "liqueur", "other"
        - abv (number): alcohol by volume percentage
        - age (string or null): e.g. "12 Years", "NAS"
        - score (integer): quality score out of 100
        - price (string): approximate retail price e.g. "$65"
        - origin (string): country of origin
        - region (string): specific region e.g. "Speyside", "Bordeaux"
        - volume (string): standard bottle size e.g. "750ml"
        - about (string): 2-3 sentences describing the liquor
        - heritage (string): 2-3 sentences about origin and history
        - profile (object): category-specific scores from 0-100:
          * For whisky: sweetness, body, richness, smokiness, finish, complexity
          * For wine: sweetness, acidity, tannin, body, fruitiness, complexity
          * For gin: juniper, citrus, floral, herbal, spice, complexity
          * For other categories: body, sweetness, complexity, finish, aroma, smoothness
        - tastingNotes (array of strings): 4-6 tasting note keywords
        - suggestedImageKeyword (string): a keyword suitable for searching a stock image

        For each text field, also provide a Korean translation version with the _ko suffix:
        - name_ko (string): Korean translation of the name
        - type_ko (string): Korean translation of the type
        - about_ko (string): Korean translation of about, 2-3 sentences
        - heritage_ko (string): Korean translation of heritage, 2-3 sentences
        - tastingNotes_ko (array of strings): Korean translation of tasting notes, 4-6 keywords

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
            mapper.readValue(cleanedText)
        } catch (e: Exception) {
            log.error("Failed to parse AI response: $cleanedText", e)
            throw RuntimeException("Failed to parse AI response as JSON: ${e.message}")
        }
    }
}
