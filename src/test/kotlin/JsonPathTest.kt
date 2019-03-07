package com.nfeld.jsonpathlite

import com.nfeld.jsonpathlite.extension.read
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
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
        assertEquals(9, result)
    }

    @Test
    fun shouldBeNullWhenReadingNull() {
        val json = "[{\"key\": null}]"
        val result = JSONArray(json).read<Int>("$[0].key")
        assertNull(result)
    }

    @Test
    fun shouldBeNullWhenValueDoesntExist() {
        val json = "[{\"key\": null}]"
        val result = JSONArray(json).read<Int>("$[0].key2")
        assertNull(result)
    }

    @Test
    fun shouldBeStringCollection() {
        val result = JsonPath("$[0].tags").readFromJson<List<String>>(LARGE_JSON)
        assertEquals(listOf("occaecat","mollit","ullamco","labore","cillum","laboris","qui"), result)
    }

    @Test
    fun shouldBeIntCollection() {
        val result = JsonPath("$[5].nums").readFromJson<List<Long>>(LARGE_JSON)
        assertEquals(listOf(1,2,3,4,5), result)
    }

    @Test
    fun shouldBeJSONArray() {
        // array in json has objects/arrays within, so should return JSONArray
        val result = JsonPath("$[0].friends").readFromJson<JSONArray>(LARGE_JSON)?.toString()
        assertEquals(JSONArray(LARGE_JSON).getJSONObject(0).getJSONArray("friends").toString(0), result.toString())
    }

    @Test
    fun shouldBeJSONObject() {
        val result = JsonPath("$[0]").readFromJson<JSONObject>(LARGE_JSON)?.toString()
        assertEquals(JSONArray(LARGE_JSON).getJSONObject(0).toString(0), result.toString())
    }

    @Test
    fun shouldDeepScanStringListResults() {
        val expected = setOf("Salazar Casey","Kathrine Osborn","Vonda Howe","Harrell Pratt","Porter Cummings",
            "Mason Leach","Spencer Valenzuela","Hope Medina","Marie Hampton","Felecia Bright",
            "Maryanne Wiggins","Marylou Caldwell","Mari Pugh","Rios Norton","Judy Good","Rosetta Stanley",
            "Margret Quinn","Lora Cotton","Gaines Henry","Dorothea Irwin")
        val result = JsonPath("$..name").readFromJson<List<String>>(LARGE_JSON)
        assertEquals(expected, result!!.toSet())
    }

    @Test
    fun shouldDeepScanDoubleListResults() {
        val expected = listOf(-85.888651, 71.831798, 78.266157, -10.214391, 32.293366)
        val result = JsonPath("$..latitude").readFromJson<List<String>>(LARGE_JSON)
        assertEquals(expected, result)
    }

    @Test
    fun shouldDeepScanArrayResult() {
        val expected = "[[\"occaecat\",\"mollit\",\"ullamco\",\"labore\",\"cillum\",\"laboris\",\"qui\"],[\"aliquip\",\"cillum\",\"qui\",\"ut\",\"ea\",\"eu\",\"reprehenderit\"],[\"nulla\",\"elit\",\"ipsum\",\"pariatur\",\"ullamco\",\"ut\",\"sint\"],[\"fugiat\",\"sit\",\"ad\",\"voluptate\",\"officia\",\"aute\",\"duis\"],[\"est\",\"dolor\",\"dolore\",\"exercitation\",\"minim\",\"dolor\",\"pariatur\"]]"
        val result = JsonPath("$..tags").readFromJson<JSONArray>(LARGE_JSON)
        assertEquals(expected, result.toString())
    }

    @Test
    fun shouldDeepScanStringListResultsFromLongerPath() {
        val expected = setOf("Felecia Bright", "Maryanne Wiggins", "Marylou Caldwell", "Marie Hampton")
        val result = JsonPath("$[2]..name").readFromJson<List<String>>(LARGE_JSON)
        assertEquals(expected, result!!.toSet())
    }
}