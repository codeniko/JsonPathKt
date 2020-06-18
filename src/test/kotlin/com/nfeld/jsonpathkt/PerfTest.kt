package com.nfeld.jsonpathkt

import io.kotest.core.spec.style.StringSpec

private const val DEFAULT_RUNS = 30
private const val DEFAULT_CALLS_PER_RUN = 80000
internal val timestamp: Long
    get() = System.currentTimeMillis()

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


class PerfTest: StringSpec({
    val json = readTree(LARGE_JSON)

})
