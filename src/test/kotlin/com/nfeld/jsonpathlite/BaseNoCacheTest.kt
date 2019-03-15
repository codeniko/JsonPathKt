package com.nfeld.jsonpathlite

import com.nfeld.jsonpathlite.cache.CacheProvider
import org.junit.jupiter.api.BeforeAll

open class BaseNoCacheTest: BaseTest() {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setupClass() {
            println("Disabling cache")
            CacheProvider.setCache(null)
        }
    }

}