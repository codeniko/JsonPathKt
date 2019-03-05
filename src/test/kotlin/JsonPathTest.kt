package com.nfeld.jsonpathlite

import com.nfeld.jsonpathlite.extension.read
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JsonPathTest : BaseTest() {

    // JsonPath::parse related tests
    @Test
    fun shouldParseJsonObject() {
        val result = JsonPath.parse(SMALL_JSON).read<Int>("$.key")
        assertEquals(JSONObject(SMALL_JSON).getInt("key"), result)
    }

    @Test
    fun shouldParseJsonArray() {
        val result = JsonPath.parse(SMALL_JSON_ARRAY).read<Int>("$[0]")
        assertEquals(JSONArray(SMALL_JSON_ARRAY).getInt(0), result)
    }

    @Test
    fun shouldThrowIfFailedToParseJson() {
        assertThrows<JSONException> {
            JsonPath.parse("5"+ SMALL_JSON).read<Int>("$.key")
        }
    }

    // JsonPath::parseOrNull related tests
    @Test
    fun shouldParseNotNullJsonObject() {
        val result = JsonPath.parseOrNull(SMALL_JSON)!!.read<Int>("$.key")
        assertEquals(JSONObject(SMALL_JSON).getInt("key"), result)
    }

    @Test
    fun shouldParseNotNullJsonArray() {
        val result = JsonPath.parseOrNull(SMALL_JSON_ARRAY)!!.read<Int>("$[0]")
        assertEquals(JSONArray(SMALL_JSON_ARRAY).getInt(0), result)
    }

    @Test
    fun shouldBeNullOnParseFailure() {
        val result = JsonPath.parseOrNull("5" + SMALL_JSON_ARRAY)
        assertNull(result)
    }


    // Other test cases
    @Test
    fun shouldBeRootObject() {
        val result = JSONObject(SMALL_JSON).read<JSONObject>("$")
        assertEquals(JSONObject(SMALL_JSON).toString(), result.toString())
    }

    @Test
    fun shouldBeRootArray() {
        val result = JSONArray(SMALL_JSON_ARRAY).read<JSONArray>("$")
        assertEquals(JSONArray(SMALL_JSON_ARRAY).toString(), result.toString())
    }

    @Test
    fun shouldBeInnerObjectFromArray() {
        val json = "[{\"key\": {\"key2\": 9}}]"
        val result = JSONArray(json).read<JSONObject>("$[0]")
        val expected = JSONArray(json).getJSONObject(0)
        assertEquals(expected.toString(), result.toString())
    }

    @Test
    fun shouldBeInnerObjectFromObject() {
        val json = "[{\"key\": {\"key2\": 9}}]"
        val result = JSONArray(json).read<JSONObject>("$[0].key")
        val expected = JSONArray(json).getJSONObject(0).getJSONObject("key")
        assertEquals(expected.toString(), result.toString())
    }

    @Test
    fun shouldBeValueTwoObjsDeep() {
        val json = "[{\"key\": {\"inner_key\": 9}}]"
        val result = JSONArray(json).read<Int>("$[0].key.inner_key")
        assertEquals(9, result)
    }

    @Test
    fun shouldReadObjKeyWithNumbers() {
        val json = "[{\"key\": {\"key2\": 9}}]"
        val result = JSONArray(json).read<Int>("$[0].key.key2")
        assertEquals(9, result)
    }

    @Test
    fun shouldReadObjKeyWithSpecialChars() {
        val key = "!@#\$%^&*()_-+={}|':;<,>?`~" // excluding ".[]"
        val json = "[{\"key\": {\"$key\": 9}}]"
        val result = JSONArray(json).read<Int>("$[0].key.$key")
        assertEquals(9, result)
    }

    @Test
    fun shouldReadObjKeyWithSpecialCharsInQuotes() {
        val key = "!@#\$%^&*()_-+=[]{}|:;<,>.?`~" // excluding '
        val json = "[{\"key\": {\"$key\": 9}}]"
        val result = JSONArray(json).read<Int>("$[0].key['$key']")
        JsonPath("$[0].key['$key']").printTokens()
        assertEquals(9, result)
    }
}