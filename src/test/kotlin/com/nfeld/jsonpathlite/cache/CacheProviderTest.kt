package com.nfeld.jsonpathlite.cache

import com.nfeld.jsonpathlite.BaseTest
import com.nfeld.jsonpathlite.JsonPath
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder


@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class CacheProviderTest: BaseTest() {

    companion object {
        @JvmStatic
        @BeforeAll
        fun resetCache() {
            println("Setting up CacheProviderTest")

            resetCacheProvider()
        }
    }

    @Test
    @Order(1)
    fun shouldBeDefaultCache() {
        val cache = CacheProvider.getCache()
        assertTrue(cache is LRUCache)
    }

    @Test
    @Order(2)
    fun shouldBeNoCache() {
        CacheProvider.setCache(null)
        assertNull(CacheProvider.getCache())
    }

    @Test
    @Order(3)
    fun shouldUseCustomCache() {
        var calledGet = false
        var calledPut = false
        val cache = object : Cache {
            override fun get(path: String): JsonPath? {
                calledGet = true
                return null
            }

            override fun put(path: String, jsonPath: JsonPath) {
                calledPut = true
            }
        }
        CacheProvider.setCache(cache)

        assertFalse(calledGet)
        assertFalse(calledPut)

        CacheProvider.getCache()?.get("")
        assertTrue(calledGet)
        assertFalse(calledPut)

        CacheProvider.getCache()?.put("", JsonPath("$"))
        assertTrue(calledGet)
        assertTrue(calledPut)
    }
}