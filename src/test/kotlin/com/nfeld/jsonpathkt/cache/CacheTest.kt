package com.nfeld.jsonpathkt.cache

import com.nfeld.jsonpathkt.JsonPath
import com.nfeld.jsonpathkt.resetCacheProvider
import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

private fun getCache(): Cache? = CacheProvider.getCache()
private fun getLruCache(): LRUCache? = getCache() as? LRUCache

class CacheTest : StringSpec({
    "should get from cache" {
        resetCacheProvider()

        val path = "$.some.path"
        assertNull(getCache()!!.get(path))

        val jsonPath = JsonPath(path)
        getCache()!!.put(path, jsonPath)
        assertEquals(jsonPath, getCache()!!.get(path))
    }

    "should put into cache" {
        resetCacheProvider()
        CacheProvider.maxCacheSize = 2
        assertEquals(2, CacheProvider.maxCacheSize)

        val path1 = "$.first"
        val compiledPath1 = JsonPath(path1)
        getCache()!!.put(path1, compiledPath1)
        var cached = getLruCache()!!.toList()
        assertTrue(cached.size == 1)
        assertEquals(path1 to compiledPath1, cached[0])

        val path2 = "$.second"
        val compiledPath2 = JsonPath(path2)
        getCache()!!.put(path2, compiledPath2)
        cached = getLruCache()!!.toList()
        assertTrue(cached.size == 2)
        assertEquals(path1 to compiledPath1, cached[0])
        assertEquals(path2 to compiledPath2, cached[1])

        // now lets test that most recently used order has changed by getting first path
        val cachedResult = getCache()!!.get(path1)
        cached = getLruCache()!!.toList()
        assertEquals(compiledPath1.tokens, cachedResult?.tokens)
        assertTrue(cached.size == 2)
        assertEquals(path1 to compiledPath1, cached[1])
        assertEquals(path2 to compiledPath2, cached[0])

        // with max cache size being 2, caching another path should remove least recently used, which is now path2
        val path3 = "$.third"
        val compiledPath3 = JsonPath(path3)
        getCache()!!.put(path3, compiledPath3)
        cached = getLruCache()!!.toList()
        assertTrue(cached.size == 2) // still needs to be 2
        assertEquals(path1 to compiledPath1, cached[0])
        assertEquals(path3 to compiledPath3, cached[1])
    }
})