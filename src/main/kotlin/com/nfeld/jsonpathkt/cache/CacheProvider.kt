package com.nfeld.jsonpathkt.cache

object CacheProvider {

    private var cache: Cache? = null
    private var useDefault = true

    /**
     * Consumer can set this to preferred max cache size.
     */
    @JvmStatic
    var maxCacheSize = 100

    /**
     * Set cache to custom implementation of [Cache].
     *
     * @param newCache cache implementation to use, or null if no cache desired.
     */
    @JvmStatic
    fun setCache(newCache: Cache?) {
        useDefault = false
        cache = newCache
    }

    internal fun getCache(): Cache? {
        if (cache == null && useDefault) {
            synchronized(this) {
                if (cache == null) {
                    cache = createDefaultCache()
                }
            }
        }
        return cache
    }

    private fun createDefaultCache(): Cache = LRUCache(maxCacheSize)
}