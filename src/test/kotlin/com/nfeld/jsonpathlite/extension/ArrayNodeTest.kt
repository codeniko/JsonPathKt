package com.nfeld.jsonpathlite.extension

import com.nfeld.jsonpathlite.BaseNoCacheTest
import com.nfeld.jsonpathlite.JsonPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArrayNodeTest : BaseNoCacheTest() {
    @Test
    fun shouldRead() {
        val jsonObj = readTree(SMALL_JSON_ARRAY)
        assertEquals(2, jsonObj.read(JsonPath("$[1]"))!!)
        assertEquals(2, jsonObj.read("$[1]")!!)
    }
}