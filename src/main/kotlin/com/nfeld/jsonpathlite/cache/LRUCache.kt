package com.nfeld.jsonpathlite.cache

import com.nfeld.jsonpathlite.JsonPath
import org.jetbrains.annotations.TestOnly
import java.util.LinkedHashMap

internal class LRUCache(private val maxCacheSize: Int): Cache {
    private val map = LRUMap()

    @Synchronized
    override fun get(path: String): JsonPath? = map.get(path)

    @Synchronized
    override fun put(path: String, jsonPath: JsonPath) {
        map.put(path, jsonPath)
    }

    @TestOnly
    internal fun toList(): List<Pair<String, JsonPath>> = map.toList()

    private inner class LRUMap : LinkedHashMap<String, JsonPath>(INITIAL_CAPACITY, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, JsonPath>?): Boolean = size > maxCacheSize
    }

    companion object {
        private const val INITIAL_CAPACITY = 16
        private const val LOAD_FACTOR = 0.75f
    }
}