package com.nfeld.jsonpathlite

import com.jayway.jsonpath.spi.cache.NOOPCache
import com.nfeld.jsonpathlite.cache.CacheProvider
import com.nfeld.jsonpathlite.extension.read
import org.json.JSONArray
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BenchmarkTest : BaseTest() {

    companion object {
        private const val DEFAULT_RUNS = 30
        private const val DEFAULT_CALLS_PER_RUN = 80000
        private var printReadmeFormat = false

        @JvmStatic
        @BeforeAll
        fun setupClass() {
            println("Setting up BenchmarkTest")

            printReadmeFormat = System.getProperty("readmeFormat")?.toBoolean() ?: false
        }

        @JvmStatic
        @BeforeEach
        fun resetCache() {
            resetCacheProvider()
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
        // first benchmarks will be using caches
        val lite = benchmarkJsonPathLite(path, callsPerRun, runs)
        val other = benchmarkJsonPath(path, callsPerRun, runs)

        // now disable caches
        CacheProvider.setCache(null)
        com.jayway.jsonpath.spi.cache.CacheProvider.setCache(NOOPCache())
        val liteNoCache = benchmarkJsonPathLite(path, callsPerRun, runs)
        val otherNoCache = benchmarkJsonPath(path, callsPerRun, runs)

        if (printReadmeFormat) {
            println("|  $path  |  ${lite} ms |  ${other} ms | $liteNoCache ms | $otherNoCache ms |")
        } else {
            println("$path   lite: ${lite}, jsonpath: ${other}   Without caches:  lite: ${liteNoCache}, jsonpath: ${otherNoCache}")
        }
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

        fun compile(path: String) {
            // first with caches
            val lite = benchmark { JsonPath(path) }
            val other = benchmark { com.jayway.jsonpath.JsonPath.compile(path) }

            // now disable caches
            CacheProvider.setCache(null)
            com.jayway.jsonpath.spi.cache.CacheProvider.setCache(NOOPCache())
            val liteNoCache = benchmark { JsonPath(path) }
            val otherNoCache = benchmark { com.jayway.jsonpath.JsonPath.compile(path) }

            val numTokens = PathCompiler.compile(path).size
            val name = "${path.length} chars, $numTokens tokens"

            if (printReadmeFormat) {
                println("|  $name  |  ${lite} ms  |  ${other} ms  | $liteNoCache ms | $otherNoCache ms |")
            } else {
                println("$name  lite: ${lite}, jsonpath: ${other}   Without caches:  lite: ${liteNoCache}, jsonpath: ${otherNoCache}")
            }
        }

        compile("$.hello")
        compile("$.hello.world[0]")
        compile("$[0].friends[1].other.a.b['c']")
        compile("$[0].friends[1].other.a.b['c'][5].niko[2].hello.world[6][9][0].id")
        compile("$[0].friends[1]..other[2].a.b['c'][5].niko[2]..hello[0].world[6][9]..['a','b','c'][0].id")
    }

    @Test
    fun benchmarkDeepScans() {
        val callsPerRun = 20000
        val runs = 10
        runBenchmarksAndPrintResults("$..tags", callsPerRun, runs)
        runBenchmarksAndPrintResults("$..name", callsPerRun, runs)
        runBenchmarksAndPrintResults("$..['email','name']", callsPerRun, runs)
        runBenchmarksAndPrintResults("$..[1]", callsPerRun, runs)
    }

    @Test
    fun benchmarkDeepScanRanges() {
        val callsPerRun = 20000
        val runs = 10
        runBenchmarksAndPrintResults("$..[:2]", callsPerRun, runs)
        runBenchmarksAndPrintResults("$..[2:]", callsPerRun, runs)
        runBenchmarksAndPrintResults("$..[1:-1]", callsPerRun, runs)
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
        runBenchmarksAndPrintResults("$[0]['tags'][0,3,5]")
    }

    @Test
    fun benchmarkMultiObjectAccess() {
        runBenchmarksAndPrintResults("$[0]['latitude','longitude','isActive']")
    }
}