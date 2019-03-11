package com.nfeld.jsonpathlite

import com.nfeld.jsonpathlite.extension.read
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JsonPathTest : BaseTest() {

    //        val result = com.jayway.jsonpath.JsonPath.parse(LARGE_JSON).read<List<String>>("$..name")

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

    @Test
    fun shouldThrowWhenParsingEmptyString() {
        assertThrows<JSONException> { JsonPath.parse("") }
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

    @Test
    fun shouldBeNullWhenParsingEmptyString() {
        assertNull(JsonPath.parseOrNull(""))
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

    @Test
    fun shouldBeLastItemInArray() {
        val result = JsonPath("$[0]['tags'][-1]").readFromJson<String>(LARGE_JSON)
        assertEquals("qui", result)
    }

    @Test
    fun shouldBeThirdToLastItemInArray() {
        val result = JsonPath("$[0]['tags'][-3]").readFromJson<String>(LARGE_JSON)
        assertEquals("cillum", result)
    }

    @Test
    fun shouldBeFirstItemInArray() {
        val result = JsonPath("$[0]['tags'][-0]").readFromJson<String>(LARGE_JSON)
        assertEquals("occaecat", result)
    }

    @Test
    fun shouldGetArrayRangeFromStart() {
        val result = JsonPath("$[0]['tags'][:3]").readFromJson<List<String>>(LARGE_JSON)
        assertEquals(listOf("occaecat","mollit","ullamco"), result)
    }

    @Test
    fun shouldGetArrayRangeFromToEnd() {
        val result = JsonPath("$[0]['tags'][5:]").readFromJson<List<String>>(LARGE_JSON)
        assertEquals(listOf("laboris","qui"), result)
    }

    @Test
    fun shouldGetArrayRangeFromBeginningToEnd() {
        val result = JsonPath("$[0]['tags'][0:]").readFromJson<List<String>>(LARGE_JSON)
        val result2 = JsonPath("$[0]['tags']").readFromJson<List<String>>(LARGE_JSON)

        assertEquals(listOf("occaecat","mollit","ullamco","labore","cillum","laboris","qui"), result)
        assertEquals(result2, result)
    }

    @Test
    fun shouldBeValues0To2() {
        val result = JsonPath("$[0]['tags'][:-4]").readFromJson<List<String>>(LARGE_JSON)
        assertEquals(listOf("occaecat","mollit","ullamco"), result)
    }

    @Test
    fun shouldBeValues5To6() {
        val result = JsonPath("$[0]['tags'][-2:]").readFromJson<List<String>>(LARGE_JSON)
        assertEquals(listOf("laboris","qui"), result)
    }

    @Test
    fun shouldBeValues4To5() {
        val result = JsonPath("$[0]['tags'][-3:-1]").readFromJson<List<String>>(LARGE_JSON)
        assertEquals(listOf("cillum","laboris"), result)
    }

    @Test
    fun shouldBeValues3To5() {
        val result = JsonPath("$[0]['tags'][3:-1]").readFromJson<List<String>>(LARGE_JSON)
        assertEquals(listOf("labore","cillum","laboris"), result)
    }

    @Test
    fun shouldBeValues1To3() {
        val result = JsonPath("$[0]['tags'][-6:4]").readFromJson<List<String>>(LARGE_JSON)
        assertEquals(listOf("mollit","ullamco","labore"), result)
    }

    @Test
    fun shouldGetArrayRange() {
        val result = JsonPath("$[0]['tags'][3:5]").readFromJson<List<String>>(LARGE_JSON)
        assertEquals(listOf("labore","cillum"), result)
    }

    @Test
    fun shouldBeMultiArray() {
        val result = JsonPath("$[0]['tags'][0, 3,5]").readFromJson<List<String>>(LARGE_JSON)
        assertEquals(listOf("occaecat","labore","laboris"), result)
    }

    @Test
    fun shouldBeMultiObject() {
        val result = JsonPath("$[0]['latitude','longitude', 'isActive']").readFromJson<JSONObject>(LARGE_JSON)
        val expected = JSONObject().apply {
            put("latitude", -85.888651)
            put("longitude", 38.287152)
            put("isActive", true)
        }.toString()
        assertEquals(expected, result.toString())
    }

    @Test
    fun shouldBeNull() {
        assertNull(JsonPath("$.key").readFromJson<Int>("()"))
        assertNull(JsonPath("$.key").readFromJson<Int>("{}"))
        assertNull(JsonPath("$[0]").readFromJson<Int>("[]"))
        assertNull(JsonPath("$[0]").readFromJson<Int>(""))
        assertNull(JsonPath("$[2][0]").readFromJson<Int>("[]"))
    }
}