package com.nfeld.jsonpathlite.extension

import com.nfeld.jsonpathlite.BaseNoCacheTest
import com.nfeld.jsonpathlite.JsonPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ObjectNodeTest : BaseNoCacheTest() {
    @Test
    fun shouldRead() {
        val jsonObj = readTree(SMALL_JSON)
        assertEquals(5, jsonObj.read(JsonPath("$['key']"))!!)
        assertEquals(5, jsonObj.read("$['key']")!!)
    }
}