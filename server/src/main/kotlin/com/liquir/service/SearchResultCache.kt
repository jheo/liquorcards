package com.liquir.service

import com.liquir.dto.AiLookupResponse
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class SearchResultCache {

    private val log = LoggerFactory.getLogger(SearchResultCache::class.java)

    private data class CachedEntry(
        val result: AiLookupResponse,
        val createdAt: Long = System.currentTimeMillis()
    )

    private val cache = ConcurrentHashMap<String, CachedEntry>()

    fun store(result: AiLookupResponse): String {
        val id = UUID.randomUUID().toString()
        cache[id] = CachedEntry(result)
        return id
    }

    fun get(id: String): AiLookupResponse? {
        val entry = cache.remove(id) ?: return null
        if (System.currentTimeMillis() - entry.createdAt > TTL_MS) {
            return null
        }
        return entry.result
    }

    @Scheduled(fixedRate = 60_000)
    fun evictExpired() {
        val now = System.currentTimeMillis()
        var count = 0
        cache.entries.removeIf { (_, entry) ->
            val expired = now - entry.createdAt > TTL_MS
            if (expired) count++
            expired
        }
        if (count > 0) {
            log.debug("Evicted {} expired search results", count)
        }
    }

    companion object {
        private const val TTL_MS = 30 * 60 * 1000L // 30 minutes
    }
}
