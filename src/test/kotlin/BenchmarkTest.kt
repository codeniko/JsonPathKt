package com.nfeld.jsonpathlite

import com.jayway.jsonpath.spi.cache.CacheProvider
import com.jayway.jsonpath.spi.cache.NOOPCache
import com.nfeld.jsonpathlite.extension.read
import org.json.JSONArray
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BenchmarkTest : BaseTest() {

    companion object {
        private const val RUNS_40K = 100000

        @JvmStatic
        @BeforeAll
        fun setupClass() {
            val yo = JsonPath("$")

            println("Setting up BenchmarkTest")

            // disable JsonPath cache for fair benchmarks
            CacheProvider.setCache(NOOPCache())
        }
    }

    private val timestamp: Long
        get() = System.currentTimeMillis()

    private fun benchmarkJsonPathLite(path: String): Long {
        val jsonArray = JSONArray(LARGE_JSON) // pre-parse json
        val t1 = timestamp
        for (i in 0..RUNS_40K) {
            jsonArray.read<String>(path)
        }
        val t2 = timestamp
        return t2 - t1
    }

    private fun benchmarkJsonPath(path: String): Long {
        val documentContext = com.jayway.jsonpath.JsonPath.parse(LARGE_JSON) // pre-parse json
        val t1 = timestamp
        for (i in 0..RUNS_40K) {
            documentContext.read<String>(path)
        }
        val t2 = timestamp
        return t2 - t1
    }

    private fun runBenchmarksAndPrintResults(path: String) {
        val lite = benchmarkJsonPathLite(path)
        val other = benchmarkJsonPath(path)
        println("$path   lite: ${lite}, jsonpath: ${other}")
    }

    @Test
    fun benchmarkDeep() {
        runBenchmarksAndPrintResults("$[0].friends[1].other.a.b['c']")
    }

    @Test
    fun benchmarkShallow() {
        runBenchmarksAndPrintResults("$[2]._id")
    }


}