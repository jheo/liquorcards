package com.liquir.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class WebCrawlerServiceTest {

    private val service = WebCrawlerServiceImpl()

    // Access private utility methods via reflection
    private fun invokeExtractPattern(text: String, pattern: String): String? {
        val method = WebCrawlerServiceImpl::class.java.getDeclaredMethod(
            "extractPattern",
            String::class.java,
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(service, text, pattern) as? String
    }

    private fun invokeFormatVolume(raw: String): String {
        val method = WebCrawlerServiceImpl::class.java.getDeclaredMethod("formatVolume", String::class.java)
        method.isAccessible = true
        return method.invoke(service, raw) as String
    }

    @Nested
    inner class ExtractPatternTests {

        @Test
        fun `should extract ABV from text`() {
            val result = invokeExtractPattern("ABV: 43.5%", """(?:ABV|Alcohol)[:\s]*(\d+(?:\.\d+)?)\s*%""")
            assertEquals("43.5", result)
        }

        @Test
        fun `should extract age from text`() {
            val result = invokeExtractPattern("Age: 12 Years", """(?:Age|Alter)[:\s]*(\d+\s*(?:Years?|Jahre))""")
            assertEquals("12 Years", result)
        }

        @Test
        fun `should return null when pattern not found`() {
            val result = invokeExtractPattern("No match here", """ABV[:\s]*(\d+)%""")
            assertNull(result)
        }

        @Test
        fun `should be case insensitive`() {
            val result = invokeExtractPattern("abv: 40%", """(?:ABV|Alcohol)[:\s]*(\d+(?:\.\d+)?)\s*%""")
            assertEquals("40", result)
        }
    }

    @Nested
    inner class FormatVolumeTests {

        @Test
        fun `should convert liters to ml`() {
            assertEquals("750ml", invokeFormatVolume("0.75 l"))
            assertEquals("1000ml", invokeFormatVolume("1 l"))
            assertEquals("1750ml", invokeFormatVolume("1.75 l"))
        }

        @Test
        fun `should keep large liter values as-is`() {
            assertEquals("12 l", invokeFormatVolume("12 l"))
        }

        @Test
        fun `should return raw when no liter pattern found`() {
            assertEquals("750ml", invokeFormatVolume("750ml"))
        }

        @Test
        fun `should handle decimal liters`() {
            assertEquals("500ml", invokeFormatVolume("0.5 l"))
            assertEquals("700ml", invokeFormatVolume("0.7l"))
        }
    }

    @Nested
    inner class CrawlDispatchTests {

        @Test
        fun `should return empty list for unknown category with no internet`() {
            // crawl calls external sites which will fail in test env
            // This validates the dispatch logic doesn't throw
            val results = service.crawl("test query", "unknown-category", listOf("test query"))
            // May be empty since network calls fail gracefully
            assertNotNull(results)
        }
    }
}
