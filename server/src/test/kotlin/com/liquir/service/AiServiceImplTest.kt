package com.liquir.service

import com.liquir.dto.AiLookupResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AiServiceImplTest {

    // Test the parseResponse logic via reflection, since it's private
    // We test the public lookupLiquor method behavior instead

    @Nested
    inner class ApiKeyValidation {

        @Test
        fun `should throw when Claude API key is blank`() {
            val service = AiServiceImpl(claudeApiKey = "", openaiApiKey = "")
            val exception = assertThrows<IllegalStateException> {
                service.lookupLiquor("Macallan 12", "claude")
            }
            assertTrue(exception.message!!.contains("Claude API key"))
        }

        @Test
        fun `should throw when OpenAI API key is blank`() {
            val service = AiServiceImpl(claudeApiKey = "", openaiApiKey = "")
            val exception = assertThrows<IllegalStateException> {
                service.lookupLiquor("Macallan 12", "openai")
            }
            assertTrue(exception.message!!.contains("OpenAI API key"))
        }

        @Test
        fun `should default to Claude provider when provider is unknown`() {
            val service = AiServiceImpl(claudeApiKey = "", openaiApiKey = "test-key")
            // Unknown provider defaults to claude, which has no key
            val exception = assertThrows<IllegalStateException> {
                service.lookupLiquor("Macallan 12", "unknown-provider")
            }
            assertTrue(exception.message!!.contains("Claude API key"))
        }
    }

    @Nested
    inner class ParseResponseTests {

        // We use a helper that invokes the private parseResponse method
        private fun invokeParseResponse(service: AiServiceImpl, text: String): AiLookupResponse {
            val method = AiServiceImpl::class.java.getDeclaredMethod("parseResponse", String::class.java)
            method.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            return method.invoke(service, text) as AiLookupResponse
        }

        private val service = AiServiceImpl(claudeApiKey = "", openaiApiKey = "")

        @Test
        fun `should parse valid JSON response`() {
            val json = """
            {
                "name": "Macallan 12",
                "type": "Single Malt Scotch Whisky",
                "category": "whisky",
                "abv": 43.0,
                "age": "12 Years",
                "score": 88,
                "price": "$75",
                "origin": "Scotland",
                "region": "Speyside",
                "volume": "750ml",
                "about": "A rich single malt.",
                "heritage": "Founded in 1824.",
                "profile": {"sweetness": 70, "body": 80},
                "tastingNotes": ["dried fruit", "sherry"],
                "suggestedImageKeyword": "macallan bottle",
                "name_ko": "맥캘란 12년",
                "type_ko": "싱글 몰트 스카치 위스키",
                "about_ko": "풍부한 싱글 몰트.",
                "heritage_ko": "1824년에 설립.",
                "tastingNotes_ko": ["건과일", "셰리"]
            }
            """.trimIndent()

            val result = invokeParseResponse(service, json)
            assertEquals("Macallan 12", result.name)
            assertEquals("Single Malt Scotch Whisky", result.type)
            assertEquals("whisky", result.category)
            assertEquals(43.0, result.abv)
            assertEquals("12 Years", result.age)
            assertEquals(88, result.score)
            assertEquals("Scotland", result.origin)
            assertEquals("Speyside", result.region)
            assertEquals("750ml", result.volume)
            assertEquals("A rich single malt.", result.about)
            assertEquals("Founded in 1824.", result.heritage)
            assertNotNull(result.profile)
            assertEquals(70, result.profile!!["sweetness"])
            assertEquals(listOf("dried fruit", "sherry"), result.tastingNotes)
            assertEquals("macallan bottle", result.suggestedImageKeyword)
            assertEquals("맥캘란 12년", result.nameKo)
            assertEquals("싱글 몰트 스카치 위스키", result.typeKo)
            assertEquals("풍부한 싱글 몰트.", result.aboutKo)
            assertEquals("1824년에 설립.", result.heritageKo)
            assertEquals(listOf("건과일", "셰리"), result.tastingNotesKo)
        }

        @Test
        fun `should strip markdown code blocks from response`() {
            val json = """
            ```json
            {"name": "Test Whisky", "type": "Whisky", "category": "whisky"}
            ```
            """.trimIndent()

            val result = invokeParseResponse(service, json)
            assertEquals("Test Whisky", result.name)
            assertEquals("Whisky", result.type)
            assertEquals("whisky", result.category)
        }

        @Test
        fun `should handle response with null optional fields`() {
            val json = """
            {"name": "Minimal", "type": "Whisky", "category": "whisky"}
            """.trimIndent()

            val result = invokeParseResponse(service, json)
            assertEquals("Minimal", result.name)
            assertNull(result.abv)
            assertNull(result.age)
            assertNull(result.score)
            assertNull(result.nameKo)
            assertNull(result.tastingNotesKo)
        }

        @Test
        fun `should throw on invalid JSON`() {
            val exception = assertThrows<java.lang.reflect.InvocationTargetException> {
                invokeParseResponse(service, "this is not json at all")
            }
            assertTrue(exception.cause is RuntimeException)
            assertTrue(exception.cause!!.message!!.contains("Failed to parse AI response"))
        }

        @Test
        fun `should handle Korean-only content without English fields`() {
            val json = """
            {
                "name": "Test",
                "type": "Type",
                "category": "whisky",
                "name_ko": "테스트",
                "type_ko": "유형",
                "about_ko": "한국어 설명",
                "heritage_ko": "한국어 역사",
                "tastingNotes_ko": ["맛1", "맛2"]
            }
            """.trimIndent()

            val result = invokeParseResponse(service, json)
            assertEquals("테스트", result.nameKo)
            assertEquals("유형", result.typeKo)
            assertEquals("한국어 설명", result.aboutKo)
            assertEquals("한국어 역사", result.heritageKo)
            assertEquals(listOf("맛1", "맛2"), result.tastingNotesKo)
        }
    }
}
