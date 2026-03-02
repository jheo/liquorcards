package com.liquir.service

import com.liquir.dto.AiLookupResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AiServiceImplTest {

    private val service = AiServiceImpl(googleApiKey = "")

    @Nested
    inner class ApiKeyValidation {

        @Test
        fun `should throw when Google API key is blank`() {
            val exception = assertThrows<IllegalStateException> {
                service.searchWithGoogle("Macallan 12")
            }
            assertTrue(exception.message!!.contains("Google API key"))
        }
    }

    @Nested
    inner class ParseResponseTests {

        private fun invokeParseResponse(svc: AiServiceImpl, text: String): AiLookupResponse {
            val method = AiServiceImpl::class.java.getDeclaredMethod("parseResponse", String::class.java)
            method.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            return method.invoke(svc, text) as AiLookupResponse
        }

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

    @Nested
    inner class ParsePhase2JsonTests {

        @Test
        fun `should parse valid JSON object`() {
            val json = """{"canonicalName": "Macallan 12", "category": "whisky"}"""
            val result = service.parsePhase2Json(json)
            assertEquals("Macallan 12", result["canonicalName"])
            assertEquals("whisky", result["category"])
        }

        @Test
        fun `should merge array of maps into single map`() {
            val json = """[
                {"canonicalName": "Macallan 12", "category": "whisky"},
                {"abv": 43.0, "origin": "Scotland"},
                {"canonicalName": "Duplicate", "region": "Speyside"}
            ]"""
            val result = service.parsePhase2Json(json)
            assertEquals("Macallan 12", result["canonicalName"]) // first wins
            assertEquals("whisky", result["category"])
            assertEquals(43.0, result["abv"])
            assertEquals("Scotland", result["origin"])
            assertEquals("Speyside", result["region"])
        }

        @Test
        fun `should treat array of strings as canonical name hint`() {
            val json = """["Macallan 12 Year Single Malt", "Macallan whisky", "Speyside single malt"]"""
            val result = service.parsePhase2Json(json)
            assertEquals("Macallan 12 Year Single Malt", result["canonicalName"])
            assertEquals("other", result["category"])
            assertEquals(0.5, result["confidence"])
            @Suppress("UNCHECKED_CAST")
            val queries = result["searchQueries"] as List<String>
            assertEquals(3, queries.size)
        }

        @Test
        fun `should throw on empty array`() {
            assertThrows<RuntimeException> {
                service.parsePhase2Json("[]")
            }
        }

        @Test
        fun `should handle whitespace around JSON`() {
            val json = """  { "canonicalName": "Test" }  """
            val result = service.parsePhase2Json(json)
            assertEquals("Test", result["canonicalName"])
        }
    }

    @Nested
    inner class ParseVolumeToMlTests {

        @Test
        fun `should parse ml format`() {
            assertEquals(750, service.parseVolumeToMl("750ml"))
            assertEquals(750, service.parseVolumeToMl("750 ml"))
            assertEquals(375, service.parseVolumeToMl("375ml"))
        }

        @Test
        fun `should parse liter format`() {
            assertEquals(1000, service.parseVolumeToMl("1L"))
            assertEquals(1750, service.parseVolumeToMl("1.75L"))
            assertEquals(1750, service.parseVolumeToMl("1.75 L"))
            assertEquals(500, service.parseVolumeToMl("0.5l"))
            assertEquals(1000, service.parseVolumeToMl("1 liter"))
        }

        @Test
        fun `should parse cl format`() {
            assertEquals(750, service.parseVolumeToMl("75cl"))
            assertEquals(700, service.parseVolumeToMl("70cl"))
            assertEquals(500, service.parseVolumeToMl("50 cl"))
        }

        @Test
        fun `should return null for empty or null input`() {
            assertNull(service.parseVolumeToMl(null))
            assertNull(service.parseVolumeToMl(""))
            assertNull(service.parseVolumeToMl("   "))
        }

        @Test
        fun `should return null for unrecognized format`() {
            assertNull(service.parseVolumeToMl("large"))
            assertNull(service.parseVolumeToMl("standard"))
        }

        @Test
        fun `should be case insensitive`() {
            assertEquals(750, service.parseVolumeToMl("750ML"))
            assertEquals(1000, service.parseVolumeToMl("1L"))
        }
    }

    @Nested
    inner class ParsePriceUsdTests {

        @Test
        fun `should parse dollar format`() {
            assertEquals(65.0, service.parsePriceUsd("$65"))
            assertEquals(65.99, service.parsePriceUsd("$65.99"))
            assertEquals(1200.0, service.parsePriceUsd("$1,200"))
        }

        @Test
        fun `should parse USD format`() {
            assertEquals(75.0, service.parsePriceUsd("USD 75"))
            assertEquals(75.0, service.parsePriceUsd("usd 75"))
        }

        @Test
        fun `should convert KRW to USD`() {
            val result = service.parsePriceUsd("₩45,000")
            assertNotNull(result)
            assertEquals(31.03, result!!, 0.01)
        }

        @Test
        fun `should convert 원 to USD`() {
            val result = service.parsePriceUsd("45000원")
            assertNotNull(result)
            assertEquals(31.03, result!!, 0.01)
        }

        @Test
        fun `should parse plain number as USD`() {
            assertEquals(50.0, service.parsePriceUsd("50"))
            assertEquals(1500.0, service.parsePriceUsd("1,500"))
        }

        @Test
        fun `should return null for empty or null input`() {
            assertNull(service.parsePriceUsd(null))
            assertNull(service.parsePriceUsd(""))
            assertNull(service.parsePriceUsd("   "))
        }

        @Test
        fun `should return null for unrecognized format`() {
            assertNull(service.parsePriceUsd("expensive"))
        }
    }

    @Nested
    inner class CleanJsonTests {

        private fun invokeCleanJson(text: String): String {
            val method = AiServiceImpl::class.java.getDeclaredMethod("cleanJson", String::class.java)
            method.isAccessible = true
            return method.invoke(service, text) as String
        }

        @Test
        fun `should strip markdown code blocks`() {
            val input = "```json\n{\"name\": \"test\"}\n```"
            val result = invokeCleanJson(input)
            assertEquals("{\"name\": \"test\"}", result)
        }

        @Test
        fun `should handle JSON with leading text`() {
            val input = "Here is the result: {\"name\": \"test\"}"
            val result = invokeCleanJson(input)
            assertEquals("{\"name\": \"test\"}", result)
        }

        @Test
        fun `should handle array JSON`() {
            val input = "[{\"name\": \"a\"}, {\"name\": \"b\"}]"
            val result = invokeCleanJson(input)
            assertEquals("[{\"name\": \"a\"}, {\"name\": \"b\"}]", result)
        }

        @Test
        fun `should prefer object over array when object comes first`() {
            val input = "{\"data\": [1,2,3]}"
            val result = invokeCleanJson(input)
            assertEquals("{\"data\": [1,2,3]}", result)
        }

        @Test
        fun `should return original text when no JSON structure found`() {
            val input = "no json here"
            val result = invokeCleanJson(input)
            assertEquals("no json here", result)
        }
    }

    @Nested
    inner class ParseAmbiguousCandidatesTests {

        private fun invokeParseAmbiguousCandidates(rawText: String): List<com.liquir.dto.DisambiguationCandidate> {
            val method = AiServiceImpl::class.java.getDeclaredMethod("parseAmbiguousCandidates", String::class.java)
            method.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            return method.invoke(service, rawText) as List<com.liquir.dto.DisambiguationCandidate>
        }

        @Test
        fun `should parse multiple candidates`() {
            val text = """
                AMBIGUOUS: yes
                CANDIDATE: The Balvenie 12 Year DoubleWood | 발베니 12년 더블우드 | 12 year old double matured
                CANDIDATE: The Balvenie 14 Year Caribbean Cask | 발베니 14년 캐리비안 캐스크 | 14 year old rum cask finished
                CANDIDATE: The Balvenie 16 Year Triple Cask | N/A | 16 year old triple cask matured
            """.trimIndent()

            val candidates = invokeParseAmbiguousCandidates(text)
            assertEquals(3, candidates.size)

            assertEquals("The Balvenie 12 Year DoubleWood", candidates[0].name)
            assertEquals("발베니 12년 더블우드", candidates[0].nameKo)
            assertEquals("12 year old double matured", candidates[0].description)

            assertEquals("The Balvenie 14 Year Caribbean Cask", candidates[1].name)
            assertEquals("발베니 14년 캐리비안 캐스크", candidates[1].nameKo)

            assertEquals("The Balvenie 16 Year Triple Cask", candidates[2].name)
            assertNull(candidates[2].nameKo) // N/A should be null
        }

        @Test
        fun `should return empty list when no candidates`() {
            val text = """
                === IDENTIFICATION ===
                Canonical Name: Macallan 12
                AMBIGUOUS: no
            """.trimIndent()
            val candidates = invokeParseAmbiguousCandidates(text)
            assertTrue(candidates.isEmpty())
        }

        @Test
        fun `should handle empty text`() {
            val candidates = invokeParseAmbiguousCandidates("")
            assertTrue(candidates.isEmpty())
        }
    }
}
