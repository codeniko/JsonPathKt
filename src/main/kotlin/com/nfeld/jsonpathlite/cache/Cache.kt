package com.nfeld.jsonpathlite.cache

import com.nfeld.jsonpathlite.JsonPath

interface Cache {
    /**
     * Retrieve an instance of [JsonPath] containing the compiled path.
     *
     * @param path path string key for cache
     * @return cached [JsonPath] instance or null if not cached
     */
    fun get(path: String): JsonPath?

    /**
     * Insert the given path and [JsonPath] as key/value pair into cache.
     *
     * @param path path string key for cache
     * @param jsonPath instance of [JsonPath] containing compiled path
     */
    fun put(path: String, jsonPath: JsonPath)
}