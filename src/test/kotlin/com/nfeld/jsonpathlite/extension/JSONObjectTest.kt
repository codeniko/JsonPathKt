package com.nfeld.jsonpathlite.extension

import com.nfeld.jsonpathlite.BaseTest
import com.nfeld.jsonpathlite.JsonPath
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JSONObjectTest : BaseTest() {
    @Test
    fun shouldRead() {
        val jsonObj = JSONObject(SMALL_JSON)
        assertEquals(5, jsonObj.read(JsonPath("$['key']"))!!)
        assertEquals(5, jsonObj.read("$['key']")!!)
    }
}