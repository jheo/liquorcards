package com.liquir.service

import com.liquir.dto.AiLookupResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SearchResultCacheTest {

    private lateinit var cache: SearchResultCache

    private fun createResult(name: String = "Macallan 12", category: String = "whisky") = AiLookupResponse(
        name = name,
        type = "Single Malt Scotch Whisky",
        category = category,
        abv = null,
        age = null,
        score = null,
        price = null,
        origin = null,
        region = null,
        volume = null,
        about = null,
        heritage = null,
        profile = null,
        tastingNotes = null,
        suggestedImageKeyword = null
    )

    private val sampleResult = createResult()

    @BeforeEach
    fun setUp() {
        cache = SearchResultCache()
    }

    @Nested
    inner class StoreAndGet {

        @Test
        fun `should store and retrieve a result`() {
            val id = cache.store(sampleResult)
            assertNotNull(id)
            assertTrue(id.isNotBlank())

            val retrieved = cache.get(id)
            assertNotNull(retrieved)
            assertEquals("Macallan 12", retrieved!!.name)
            assertEquals("whisky", retrieved.category)
        }

        @Test
        fun `should generate unique ids for each store`() {
            val id1 = cache.store(sampleResult)
            val id2 = cache.store(sampleResult)
            assertNotEquals(id1, id2)
        }

        @Test
        fun `should return null for non-existent id`() {
            val result = cache.get("non-existent-id")
            assertNull(result)
        }
    }

    @Nested
    inner class OneTimeUse {

        @Test
        fun `should remove result after first get (one-time use)`() {
            val id = cache.store(sampleResult)
            val first = cache.get(id)
            assertNotNull(first)

            val second = cache.get(id)
            assertNull(second)
        }
    }

    @Nested
    inner class EvictExpired {

        @Test
        fun `should not throw when evicting with empty cache`() {
            assertDoesNotThrow { cache.evictExpired() }
        }

        @Test
        fun `should keep non-expired entries during eviction`() {
            val id = cache.store(sampleResult)
            cache.evictExpired()
            // Entry is still fresh, so get should still return it
            val result = cache.get(id)
            assertNotNull(result)
        }
    }

    @Nested
    inner class MultipleEntries {

        @Test
        fun `should handle multiple independent entries`() {
            val result1 = createResult("Whisky A", "whisky")
            val result2 = createResult("Wine B", "wine")
            val result3 = createResult("Beer C", "beer")

            val id1 = cache.store(result1)
            val id2 = cache.store(result2)
            val id3 = cache.store(result3)

            assertEquals("Whisky A", cache.get(id1)!!.name)
            assertEquals("Wine B", cache.get(id2)!!.name)
            assertEquals("Beer C", cache.get(id3)!!.name)

            // All consumed
            assertNull(cache.get(id1))
            assertNull(cache.get(id2))
            assertNull(cache.get(id3))
        }
    }
}
