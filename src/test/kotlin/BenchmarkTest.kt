package com.nfeld.jsonpathlite

import com.jayway.jsonpath.spi.cache.CacheProvider
import com.jayway.jsonpath.spi.cache.NOOPCache
import com.nfeld.jsonpathlite.extension.read
import org.json.JSONArray
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class BenchmarkTest : BaseTest() {

    companion object {
        private const val DEFAULT_RUNS = 30
        private const val DEFAULT_CALLS_PER_RUN = 80000

        @JvmStatic
        @BeforeAll
        fun setupClass() {
            println("Setting up BenchmarkTest")

            // disable JsonPath cache for fair benchmarks
            CacheProvider.setCache(NOOPCache())
        }
    }

    private val timestamp: Long
        get() = System.currentTimeMillis()

    private inline fun benchmark(callsPerRun: Int = DEFAULT_CALLS_PER_RUN, runs: Int = DEFAULT_RUNS, f: () -> Unit): Long {
        // warmup
        f()

        val times = mutableListOf<Long>()

        for (i in 0 until runs) {
            val t1 = timestamp
            for (k in 0 until callsPerRun) {
                f()
            }
            val t2 = timestamp
            times.add(t2 - t1)
        }

        return times.average().toLong()
    }

    private fun benchmarkJsonPathLite(path: String, callsPerRun: Int = DEFAULT_CALLS_PER_RUN, runs: Int = DEFAULT_RUNS): Long {
        val jsonArray = JSONArray(LARGE_JSON) // pre-parse json
        return benchmark(callsPerRun, runs) { jsonArray.read<String>(path) }
    }

    private fun benchmarkJsonPath(path: String, callsPerRun: Int = DEFAULT_CALLS_PER_RUN, runs: Int = DEFAULT_RUNS): Long {
        val documentContext = com.jayway.jsonpath.JsonPath.parse(LARGE_JSON) // pre-parse json
        return benchmark(callsPerRun, runs) { documentContext.read<String>(path) }
    }

    private fun runBenchmarksAndPrintResults(path: String, callsPerRun: Int = DEFAULT_CALLS_PER_RUN, runs: Int = DEFAULT_RUNS) {
        val lite = benchmarkJsonPathLite(path, callsPerRun, runs)
        val other = benchmarkJsonPath(path, callsPerRun, runs)
        println("$path   lite: ${lite}, jsonpath: ${other}")
    }

    @Test
    fun benchmarkDeepPath() {
        runBenchmarksAndPrintResults("$[0].friends[1].other.a.b['c']")
    }

    @Test
    fun benchmarkShallowPath() {
        runBenchmarksAndPrintResults("$[2]._id")
    }

    @Test
    fun benchmarkPathCompile() {
        // short path length
        var lite = benchmark { JsonPath("$.hello['world']") }
        var other = benchmark { com.jayway.jsonpath.JsonPath.compile("$.hello['world']") }
        println("short path compile time    lite: ${lite}, jsonpath: ${other}")

        // medium path length
        lite = benchmark { JsonPath("$[0].friends[1].other.a.b['c']") }
        other = benchmark { com.jayway.jsonpath.JsonPath.compile("$[0].friends[1].other.a.b['c']") }
        println("medium path compile time   lite: ${lite}, jsonpath: ${other}")

        // long path length
        lite = benchmark { JsonPath("$[0].friends[1].other.a.b['c'][5].niko[2].hello.world[6][9][0].id") }
        other = benchmark { com.jayway.jsonpath.JsonPath.compile("$[0].friends[1].other.a.b['c'][5].niko[2].hello.world[6][9][0].id") }
        println("long path compile time     lite: ${lite}, jsonpath: ${other}")
    }

    @Test
    fun benchmarkDeepScan() {
        val callsPerRun = 20000
        val runs = 10
        runBenchmarksAndPrintResults("$..tags", callsPerRun, runs)
        runBenchmarksAndPrintResults("$..name", callsPerRun, runs)
    }

    @Test
    fun benchmarkFromLastArrayAccess() {
        runBenchmarksAndPrintResults("$[0]['tags'][-3]")
    }

    @Test
    fun benchmarkArrayRangeFromStart() {
        runBenchmarksAndPrintResults("$[0]['tags'][:3]")
    }

    @Test
    fun benchmarkArrayRangeToEnd() {
        runBenchmarksAndPrintResults("$[0]['tags'][3:]")
    }

    @Test
    fun benchmarkArrayRange() {
        runBenchmarksAndPrintResults("$[0]['tags'][3:5]")
    }

    @Test
    fun benchmarkMultiArrayAccess() {
        runBenchmarksAndPrintResults("$[0]['tags'][0,3, 5]")
    }

    @Test
    fun benchmarkMultiObjectAccess() {
        runBenchmarksAndPrintResults("$[0]['latitude','longitude', 'isActive']")
    }
}