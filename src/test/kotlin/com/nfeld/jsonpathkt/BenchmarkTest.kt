package com.nfeld.jsonpathkt

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.spi.cache.NOOPCache
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.nfeld.jsonpathkt.cache.CacheProvider
import io.kotest.core.spec.style.StringSpec

private const val DEFAULT_RUNS = 30
private const val DEFAULT_CALLS_PER_RUN = 80000
private var printReadmeFormat = false

private fun benchmark(callsPerRun: Int = DEFAULT_CALLS_PER_RUN, runs: Int = DEFAULT_RUNS, f: () -> Unit): Long {
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

private fun benchmarkJsonPathKt(path: String, callsPerRun: Int = DEFAULT_CALLS_PER_RUN, runs: Int = DEFAULT_RUNS): Long {
    val json = JsonPath.parse(LARGE_JSON)!! // pre-parse json
//    println("our    result: " + JsonPath(path).readFromJson<Any>(json).toString())
    return benchmark(callsPerRun, runs) { JsonPath(path).readFromJson<Any>(json) }
}

private fun benchmarkJsonPath(path: String, callsPerRun: Int = DEFAULT_CALLS_PER_RUN, runs: Int = DEFAULT_RUNS): Long {
    val jaywayConfig = Configuration.defaultConfiguration().jsonProvider(JacksonJsonProvider())
    val documentContext = com.jayway.jsonpath.JsonPath.parse(LARGE_JSON, jaywayConfig) // pre-parse json
//    println("jayway result: " + documentContext.read<Any>(path).toString())
    return benchmark(callsPerRun, runs) { documentContext.read<Any>(path) }
}

private fun runBenchmarksAndPrintResults(path: String, callsPerRun: Int = DEFAULT_CALLS_PER_RUN, runs: Int = DEFAULT_RUNS) {
    // reset caches to initial position, default on
    resetCaches()

    // first benchmarks will be using caches
    val kt = benchmarkJsonPathKt(path, callsPerRun, runs)
    val other = benchmarkJsonPath(path, callsPerRun, runs)

    // now disable caches
    CacheProvider.setCache(null)
    resetJaywayCacheProvider()
    com.jayway.jsonpath.spi.cache.CacheProvider.setCache(NOOPCache())
    val ktNoCache = benchmarkJsonPathKt(path, callsPerRun, runs)
    val otherNoCache = benchmarkJsonPath(path, callsPerRun, runs)

    if (printReadmeFormat) {
        println("|  $path  |  $ktNoCache ms *($kt ms w/ cache)* |  $otherNoCache ms *($other ms w/ cache)*  |")
    } else {
        println("$path   kt: ${kt}, jsonpath: $other     Without caches:  kt: ${ktNoCache}, jsonpath: $otherNoCache")
    }
}

private fun resetCaches() {
    resetCacheProvider()
    resetJaywayCacheProvider()
}

class BenchmarkTest : StringSpec({

    beforeSpec {
        println("Setting up BenchmarkTest")

        printReadmeFormat = System.getProperty("readmeFormat")?.toBoolean() ?: false
    }

    "benchmark deep path" {
        runBenchmarksAndPrintResults("$[0].friends[1].other.a.b['c']")
    }

    "benchmark shallow path" {
        runBenchmarksAndPrintResults("$[2]._id")
    }

    "benchmark compiling path" {

        fun compile(path: String) {
            resetCaches()

            // first with caches
            val kt = benchmark { JsonPath(path) }
            val other = benchmark { com.jayway.jsonpath.JsonPath.compile(path) }

            // now disable caches
            CacheProvider.setCache(null)
            resetJaywayCacheProvider()
            com.jayway.jsonpath.spi.cache.CacheProvider.setCache(NOOPCache())

            val ktNoCache = benchmark { JsonPath(path) }
            val otherNoCache = benchmark { com.jayway.jsonpath.JsonPath.compile(path) }

            val numTokens = PathCompiler.compile(path).size
            val name = "${path.length} chars, $numTokens tokens"

            if (printReadmeFormat) {
                println("|  $name  |  $ktNoCache ms *($kt ms w/ cache)* |  $otherNoCache ms *($other ms w/ cache)* |")
            } else {
                println("$name  kt: ${kt}, jsonpath: $other     Without caches:  kt: ${ktNoCache}, jsonpath: $otherNoCache")
            }
        }

        compile("$.hello")
        compile("$.hello.world[0]")
        compile("$[0].friends[1].other.a.b['c']")
        compile("$[0].friends[1].other.a.b['c'][5].niko[2].hello.world[6][9][0].id")
        compile("$[0].friends[1]..other[2].a.b['c'][5].niko[2]..hello[0].world[6][9]..['a','b','c'][0].id")
    }

    "benchmark deep scans" {
        val callsPerRun = 20000
        val runs = 10
        runBenchmarksAndPrintResults("$..name", callsPerRun, runs)
        runBenchmarksAndPrintResults("$..['email','name']", callsPerRun, runs)
        runBenchmarksAndPrintResults("$..[1]", callsPerRun, runs)
    }

    "benchmark deep scan ranges" {
        val callsPerRun = 20000
        val runs = 10
        runBenchmarksAndPrintResults("$..[:2]", callsPerRun, runs)
        runBenchmarksAndPrintResults("$..[2:]", callsPerRun, runs)

        // jayway jsonpath gives empty response for this so not valid comparison
        // runBenchmarksAndPrintResults("$..[1:-1]", callsPerRun, runs)
    }

    "benchmark array access from end element" {
        runBenchmarksAndPrintResults("$[0]['tags'][-3]")
    }

    "benchmark array range from start" {
        runBenchmarksAndPrintResults("$[0]['tags'][:3]")
    }

    "benchmark array range to end element" {
        runBenchmarksAndPrintResults("$[0]['tags'][3:]")
    }

    "benchmark array range" {
        runBenchmarksAndPrintResults("$[0]['tags'][3:5]")
    }

    "benchmark multi array access" {
        runBenchmarksAndPrintResults("$[0]['tags'][0,3,5]")
    }

    "benchmark multi object access" {
        runBenchmarksAndPrintResults("$[0]['latitude','longitude','isActive']")
    }

    "benchmark wildcard" {
        runBenchmarksAndPrintResults("$[0]['tags'].*")
    }

    "benchmark recursive wildcard" {
        runBenchmarksAndPrintResults("$[0]..*")
    }
})