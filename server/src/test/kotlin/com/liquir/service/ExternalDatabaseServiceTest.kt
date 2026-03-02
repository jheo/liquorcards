package com.liquir.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExternalDatabaseServiceTest {

    private val webCrawlerService = object : WebCrawlerService {
        override fun crawl(name: String, category: String, searchQueries: List<String>): List<ExternalLookupData> = emptyList()
    }

    private val service = ExternalDatabaseServiceImpl(
        untappdClientId = "",
        untappdClientSecret = "",
        webCrawlerService = webCrawlerService
    )

    // Access private methods via reflection
    private fun invokeIsRelevantResult(result: ExternalLookupData, queries: List<String>): Boolean {
        val method = ExternalDatabaseServiceImpl::class.java.getDeclaredMethod(
            "isRelevantResult",
            ExternalLookupData::class.java,
            List::class.java
        )
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(service, result, queries) as Boolean
    }

    private fun invokeIsAbbreviationOf(abbr: String, text: String): Boolean {
        val method = ExternalDatabaseServiceImpl::class.java.getDeclaredMethod(
            "isAbbreviationOf",
            String::class.java,
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(service, abbr, text) as Boolean
    }

    private fun invokeMapCategory(categories: String?): String {
        val method = ExternalDatabaseServiceImpl::class.java.getDeclaredMethod(
            "mapOpenFoodFactsCategory",
            String::class.java
        )
        method.isAccessible = true
        return method.invoke(service, categories) as String
    }

    @Nested
    inner class IsRelevantResultTests {

        @Test
        fun `should return true when result name contains query word`() {
            val result = ExternalLookupData(name = "Macallan 12 Year Single Malt")
            assertTrue(invokeIsRelevantResult(result, listOf("Macallan 12")))
        }

        @Test
        fun `should return true when brand contains query word`() {
            val result = ExternalLookupData(brand = "Macallan")
            assertTrue(invokeIsRelevantResult(result, listOf("Macallan 12")))
        }

        @Test
        fun `should return false when no query words match`() {
            val result = ExternalLookupData(name = "Glenfiddich 15")
            assertFalse(invokeIsRelevantResult(result, listOf("Macallan 12")))
        }

        @Test
        fun `should return true for blank result name`() {
            val result = ExternalLookupData(name = "", brand = "")
            assertTrue(invokeIsRelevantResult(result, listOf("anything")))
        }

        @Test
        fun `should match against any search query`() {
            val result = ExternalLookupData(name = "OBC Cosmos Ale")
            assertTrue(invokeIsRelevantResult(result, listOf("Macallan 12", "OBC Cosmos")))
        }

        @Test
        fun `should match abbreviations`() {
            val result = ExternalLookupData(name = "Original Beer Company Cosmos")
            assertTrue(invokeIsRelevantResult(result, listOf("obc cosmos")))
        }

        @Test
        fun `should filter out stop words from query`() {
            val result = ExternalLookupData(name = "Macallan Single Malt")
            // "single" and "malt" are stop words, "macallan" should still match
            assertTrue(invokeIsRelevantResult(result, listOf("Macallan Single Malt")))
        }
    }

    @Nested
    inner class IsAbbreviationOfTests {

        @Test
        fun `should match OBC to Original Beer Company`() {
            assertTrue(invokeIsAbbreviationOf("obc", "original beer company"))
        }

        @Test
        fun `should match abbreviation at different starting positions`() {
            assertTrue(invokeIsAbbreviationOf("bc", "original beer company"))
        }

        @Test
        fun `should not match when abbreviation is too short`() {
            assertFalse(invokeIsAbbreviationOf("o", "original beer company"))
        }

        @Test
        fun `should not match when not enough words`() {
            assertFalse(invokeIsAbbreviationOf("abc", "just two"))
        }

        @Test
        fun `should not match when letters don't correspond`() {
            assertFalse(invokeIsAbbreviationOf("xyz", "original beer company"))
        }

        @Test
        fun `should handle hyphenated text`() {
            assertTrue(invokeIsAbbreviationOf("sm", "single-malt whisky"))
        }
    }

    @Nested
    inner class MapCategoryTests {

        @Test
        fun `should map whisky categories`() {
            assertEquals("whisky", invokeMapCategory("Scotch Whisky"))
            assertEquals("whisky", invokeMapCategory("Bourbon"))
            assertEquals("whisky", invokeMapCategory("American Whiskey"))
        }

        @Test
        fun `should map wine categories`() {
            assertEquals("wine", invokeMapCategory("Red Wine"))
            assertEquals("wine", invokeMapCategory("Vin Rouge"))
        }

        @Test
        fun `should map beer categories`() {
            assertEquals("beer", invokeMapCategory("India Pale Ale"))
            assertEquals("beer", invokeMapCategory("Lager Beer"))
            assertEquals("beer", invokeMapCategory("Bière artisanale"))
        }

        @Test
        fun `should map spirit categories`() {
            assertEquals("gin", invokeMapCategory("London Dry Gin"))
            assertEquals("vodka", invokeMapCategory("Premium Vodka"))
            assertEquals("rum", invokeMapCategory("Dark Rum"))
            assertEquals("rum", invokeMapCategory("Rhum agricole"))
            assertEquals("tequila", invokeMapCategory("Tequila Reposado"))
            assertEquals("brandy", invokeMapCategory("Cognac VSOP"))
        }

        @Test
        fun `should map liqueur category`() {
            assertEquals("liqueur", invokeMapCategory("Fruit Liqueur"))
        }

        @Test
        fun `should map sake category`() {
            assertEquals("sake", invokeMapCategory("Japanese Sake"))
        }

        @Test
        fun `should return other for null`() {
            assertEquals("other", invokeMapCategory(null))
        }

        @Test
        fun `should return other for unknown categories`() {
            assertEquals("other", invokeMapCategory("Random Food Product"))
        }
    }
}
