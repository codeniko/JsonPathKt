package com.nfeld.jsonpathlite

import com.nfeld.jsonpathlite.extension.read
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.nfeld.jsonpathlite.util.JacksonUtil
import com.nfeld.jsonpathlite.util.createObjectNode


class JsonPathTest : BaseNoCacheTest() {

    data class PojoClass1(val key: String)
    data class PojoClass2(val key: List<Any?>)
    data class PojoClass3(val key: Map<String, Int>, val default: Int)

    // JsonPath::parse related tests
    @Test
    fun shouldParseNotNullObjectNode() {
        val result = JsonPath.parse(SMALL_JSON)?.read<Int>("$.key")
        assertEquals(5, result)
    }

    @Test
    fun shouldParseNotNullArrayNode() {
        val result = JsonPath.parse(SMALL_JSON_ARRAY)?.read<Int>("$[0]")
        assertEquals(1, result)
    }

    @Test
    fun shouldBeNullOnParseFailure() {
        val result = JsonPath.parse("5" + SMALL_JSON_ARRAY)
        assertNull(result)
    }

    @Test
    fun shouldBeNullWhenParsingEmptyString() {
        assertNull(JsonPath.parse(""))
    }

    @Test
    fun shouldBeNullWhenParsingStringAsRootButWithoutQuotes() {
        assertNull(JsonPath.parse("hello"))
    }

    @Test
    fun shouldBeNullWhenParsingSymbolAsRoot() {
        assertNull(JsonPath.parse("$"))
    }

    @Test
    fun shouldParseAndReadPrimitiveValuesAsRoot() {
        assertEquals("7", JsonPath.parse("7").toString())
        assertEquals(7, JsonPath.parse("7")?.read<Int>("$"))
        assertEquals("7.76", JsonPath.parse("7.76").toString())
        assertEquals(7.76, JsonPath.parse("7.76")?.read<Double>("$"))
        assertEquals("true", JsonPath.parse("true").toString())
        assertEquals(true, JsonPath.parse("true")?.read<Boolean>("$"))
        assertEquals("false", JsonPath.parse("false").toString())
        assertEquals(false, JsonPath.parse("false")?.read<Boolean>("$"))
        assertEquals("\"hello\"", JsonPath.parse("\"hello\"").toString())
        assertEquals("hello", JsonPath.parse("\"hello\"")!!.read<String>("$"))
    }


    // Other test cases
    @Test
    fun shouldBeRootObject() {
        val result = readTree(SMALL_JSON).read<ObjectNode>("$")
        assertEquals(readTree(SMALL_JSON).toString(), result.toString())
    }

    @Test
    fun shouldBeRootArray() {
        val result = readTree(SMALL_JSON_ARRAY).read<ArrayNode>("$")
        assertEquals(readTree(SMALL_JSON_ARRAY).toString(), result.toString())
    }

    @Test
    fun shouldBeInnerObjectFromArray() {
        val json = "[{\"key\": {\"key2\": 9}}]"
        val result = readTree(json).read<ObjectNode>("$[0]")
        val expected = readTree(json).get(0)
        assertEquals(expected.toString(), result.toString())
    }

    @Test
    fun shouldBeInnerObjectFromObject() {
        val json = "[{\"key\": {\"key2\": 9}}]"
        val result = readTree(json).read<ObjectNode>("$[0].key")
        val expected = readTree(json).get(0).get("key")
        assertEquals(expected.toString(), result.toString())
    }

    @Test
    fun shouldBeValueTwoObjsDeep() {
        val json = "[{\"key\": {\"inner_key\": 9}}]"
        val result = readTree(json).read<Int>("$[0].key.inner_key")
        assertEquals(9, result)
    }

    @Test
    fun shouldReadObjKeyWithNumbers() {
        val json = "[{\"key\": {\"key2\": 9}}]"
        val result = readTree(json).read<Int>("$[0].key.key2")
        assertEquals(9, result)
    }

    @Test
    fun shouldReadObjKeyWithSpecialChars() {
        val key = "!@#\$%^&*()_-+={}|':;<,>?`~" // excluding ".[]"
        val json = "[{\"key\": {\"$key\": 9}}]"
        val result = readTree(json).read<Int>("$[0].key.$key")
        assertEquals(9, result)
    }

    @Test
    fun shouldReadObjKeyWithSpecialCharsInQuotes() {
        val key = "!@#\$%^&*()_-+=[]{}|:;<,>.?`~" // excluding '
        val json = "[{\"key\": {\"$key\": 9}}]"
        val result = readTree(json).read<Int>("$[0].key['$key']")
        assertEquals(9, result)
    }

    @Test
    fun shouldBeNullWhenReadingNull() {
        val json = "[{\"key\": null}]"
        val result = readTree(json).read<Int>("$[0].key")
        assertNull(result)
    }

    @Test
    fun shouldBeNullWhenValueDoesntExist() {
        val json = "[{\"key\": null}]"
        val result = readTree(json).read<Int>("$[0].key2")
        assertNull(result)
    }

    @Test
    fun shouldCastAsString() {
        val json = """{"key": [1, "random", null, 1.765]}"""
        val result = readTree(json).read<String>("$.key[1]")
        assertEquals("random", result)
    }

    @Test
    fun shouldCastAsInt() {
        val json = """{"key": [1, "random", null, 1.765]}"""
        val result = readTree(json).read<Int>("$.key[0]")
        assertEquals(1, result)
    }

    @Test
    fun shouldCastAsDouble() {
        val json = """{"key": [1, "random", null, 1.765]}"""
        val result = readTree(json).read<Double>("$.key[3]")
        assertEquals(1.765, result)
    }

    @Test
    fun shouldCastAsFloat() {
        val json = """{"key": [1, "random", null, 1.765]}"""
        val result = readTree(json).read<Float>("$.key[3]")
        assertEquals(1.765f, result)
    }

    @Test
    fun shouldCastAsPOJO() {
        val result1 = readTree("""{"key": "value"}""").read<PojoClass1>("$")
        assertEquals(PojoClass1(key = "value"), result1)

        val result2 = readTree("""{"key": [1, "random", null, 1.765]}""").read<PojoClass2>("$")
        assertEquals(PojoClass2(key = listOf<Any?>(1, "random", null, 1.765)), result2)

        val result3 = readTree("""{"key": { "a": 1, "b": 2 }, "default": 3}""").read<PojoClass3>("$")
        assertEquals(PojoClass3(key = mapOf("a" to 1, "b" to 2), default = 3), result3)
    }

    @Test
    fun shouldIncludeNullsInListCollection() {
        val json = """{"key": [1, "random", null, 1.765]}"""
        val result = readTree(json).read<List<Any?>>("$.key")
        assertEquals(listOf(1, "random", null, 1.765), result)
    }

    @Test
    fun shouldBeStringCollection() {
        val result = JsonPath("$[0].tags").readFromJson<List<String>>(LARGE_JSON)
        assertEquals(listOf("occaecat","mollit","ullamco","labore","cillum","laboris","qui"), result)
    }

    @Test
    fun shouldBeStringCollectionButInArrayNode() {
        val result = JsonPath("$[0].tags").readFromJson<ArrayNode>(LARGE_JSON)
        assertEquals(readTree("""["occaecat","mollit","ullamco","labore","cillum","laboris","qui"]"""), result)
    }

    @Test
    fun shouldBeIntCollection() {
        val result = JsonPath("$[5].nums").readFromJson<List<Int>>(LARGE_JSON)
        assertEquals(listOf(1,2,3,4,5), result)
    }

    @Test
    fun shouldBeLongCollection() {
        val result = JsonPath("$[5].nums").readFromJson<List<Long>>(LARGE_JSON)
        assertEquals(listOf(1L,2L,3L,4L,5L), result)
    }

    @Test
    fun shouldBeMap() {
        val result = JsonPath("$").readFromJson<Map<String, Any>>("""{"a": {"b": "yo"}}}""")
        assertEquals(mapOf("a" to mapOf("b" to "yo")), result)
    }

    @Test
    fun shouldBeArrayNode() {
        // array in json has objects/arrays within, so should return ArrayNode
        val result = JsonPath("$[0].friends").readFromJson<ArrayNode>(LARGE_JSON)?.toString()
        assertEquals(readTree(LARGE_JSON).get(0).get("friends").toString(), result.toString())
    }

    @Test
    fun shouldBeObjectNode() {
        val result = JsonPath("$[0]").readFromJson<ObjectNode>(LARGE_JSON)?.toString()
        assertEquals(readTree(LARGE_JSON).get(0).toString(), result.toString())
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
        val result = JsonPath("$..latitude").readFromJson<List<Double>>(LARGE_JSON)
        assertEquals(expected, result)
    }

    @Test
    fun shouldDeepScanArrayResult() {
        val expected = "[[\"occaecat\",\"mollit\",\"ullamco\",\"labore\",\"cillum\",\"laboris\",\"qui\"],[\"aliquip\",\"cillum\",\"qui\",\"ut\",\"ea\",\"eu\",\"reprehenderit\"],[\"nulla\",\"elit\",\"ipsum\",\"pariatur\",\"ullamco\",\"ut\",\"sint\"],[\"fugiat\",\"sit\",\"ad\",\"voluptate\",\"officia\",\"aute\",\"duis\"],[\"est\",\"dolor\",\"dolore\",\"exercitation\",\"minim\",\"dolor\",\"pariatur\"]]"
        val result = JsonPath("$..tags").readFromJson<ArrayNode>(LARGE_JSON)
        assertEquals(expected, result.toString())
    }

    @Test
    fun shouldDeepScanStringListResultsFromLongerPath() {
        val expected = setOf("Felecia Bright", "Maryanne Wiggins", "Marylou Caldwell", "Marie Hampton")
        val result = JsonPath("$[2]..name").readFromJson<List<String>>(LARGE_JSON)
        assertEquals(expected, result!!.toSet())
    }

    @Test
    fun shouldDeepScanBracketNotation() {
        val expected = setOf("Felecia Bright", "Maryanne Wiggins", "Marylou Caldwell", "Marie Hampton")
        val result = JsonPath("$[2]..['name']").readFromJson<List<String>>(LARGE_JSON)
        assertEquals(expected, result!!.toSet())
    }

    @Test
    fun shouldDeepScanArrayResultAccessing() {
        val expected = """["nulla",{"id":0,"name":"Felecia Bright","other":{"a":{"b":{"c":"yo"}}}}]"""
        val result = JsonPath("$[2]..[0]").readFromJson<ArrayNode>(LARGE_JSON)
        assertEquals(expected, result!!.toString())
    }

    @Test
    fun shouldDeepScanArrayResultFromLast() {
        val expected = """["sint",{"id":2,"name":"Marylou Caldwell","other":{"a":{"b":{"c":"yo"}}}}]"""
        val result = JsonPath("$[2]..[-1]").readFromJson<ArrayNode>(LARGE_JSON)
        assertEquals(expected, result!!.toString())
    }

    @Test
    fun shouldDeepScanArrayMultiResultAccessing() {
        val expected = """["nulla","ipsum",{"id":0,"name":"Felecia Bright","other":{"a":{"b":{"c":"yo"}}}},{"id":2,"name":"Marylou Caldwell","other":{"a":{"b":{"c":"yo"}}}}]"""
        val result = JsonPath("$[2]..[0,2]").readFromJson<ArrayNode>(LARGE_JSON)
        assertEquals(expected, result!!.toString())
    }

    @Test
    fun shouldDeepScanArrayMultiResultWithNegativeIndex() {
        val expected = """["nulla","ut",{"id":0,"name":"Felecia Bright","other":{"a":{"b":{"c":"yo"}}}},{"name":"Maryanne Wiggins","other":{"a":{"b":{"c":"yo"}}}}]"""
        val result = JsonPath("$[2]..[0, -2]").readFromJson<ArrayNode>(LARGE_JSON)
        assertEquals(expected, result!!.toString())
    }

    @Test
    fun shouldDeepScanArrayRangeResult() {
        val expected = """["nulla","elit",{"id":0,"name":"Felecia Bright","other":{"a":{"b":{"c":"yo"}}}},{"name":"Maryanne Wiggins","other":{"a":{"b":{"c":"yo"}}}}]"""
        val result = JsonPath("$[2]..[0:2]").readFromJson<ArrayNode>(LARGE_JSON)
        assertEquals(expected, result!!.toString())
    }

    @Test
    fun shouldDeepScanLengthBasedToEnd() {
        val expected = """["ipsum","pariatur","ullamco","ut","sint",{"id":2,"name":"Marylou Caldwell","other":{"a":{"b":{"c":"yo"}}}}]"""
        val result = JsonPath("$[2]..[2:]").readFromJson<ArrayNode>(LARGE_JSON)
        assertEquals(expected, result!!.toString())
    }

    @Test
    fun shouldDeepScanLengthBasedToNegativeEnd() {
        val expected = """["nulla","elit","ipsum","pariatur","ullamco","ut",{"id":0,"name":"Felecia Bright","other":{"a":{"b":{"c":"yo"}}}},{"name":"Maryanne Wiggins","other":{"a":{"b":{"c":"yo"}}}}]"""
        val result = JsonPath("$[2]..[:-1]").readFromJson<ArrayNode>(LARGE_JSON)
        assertEquals(expected, result!!.toString())
    }

    @Test
    fun shouldDeepScanLengthBasedNegativeStart() {
        val expected = """["ut","sint",{"name":"Maryanne Wiggins","other":{"a":{"b":{"c":"yo"}}}},{"id":2,"name":"Marylou Caldwell","other":{"a":{"b":{"c":"yo"}}}}]"""
        val result = JsonPath("$[2]..[-2:]").readFromJson<ArrayNode>(LARGE_JSON)
        assertEquals(expected, result!!.toString())
    }

    @Test
    fun shouldDeepScanLengthBasedStartToNegativeEnd() {
        val expected = """["elit","ipsum","pariatur","ullamco","ut",{"name":"Maryanne Wiggins","other":{"a":{"b":{"c":"yo"}}}}]"""
        val result = JsonPath("$[2]..[1:-1]").readFromJson<ArrayNode>(LARGE_JSON)
        assertEquals(expected, result!!.toString())
    }

    @Test
    fun shouldDeepScanLengthBasedNegativeStartToEnd() {
        val expected = """[{"name":"Maryanne Wiggins","other":{"a":{"b":{"c":"yo"}}}},{"id":2,"name":"Marylou Caldwell","other":{"a":{"b":{"c":"yo"}}}}]"""
        val result = JsonPath("$[2]..[-2:5]").readFromJson<ArrayNode>(LARGE_JSON)
        assertEquals(expected, result!!.toString())
    }

    @Test
    fun shouldDeepScanLengthBasedNegativeStartToNegativeEnd() {
        val expected = """["ut",{"name":"Maryanne Wiggins","other":{"a":{"b":{"c":"yo"}}}}]"""
        val result = JsonPath("$[2]..[-2:-1]").readFromJson<ArrayNode>(LARGE_JSON)
        assertEquals(expected, result!!.toString())
    }

    @Test
    fun shouldDeepScanListOfKeysOnSameLevel() {
        val expected = readTree("[{\n" +
                "  \"name\": \"Marie Hampton\"\n" +
                "}, {\n" +
                "  \"name\": \"Felecia Bright\",\n" +
                "  \"id\": 0\n" +
                "}, {\n" +
                "  \"name\": \"Maryanne Wiggins\"\n" +
                "}, {\n" +
                "  \"name\": \"Marylou Caldwell\",\n" +
                "  \"id\": 2\n" +
                "}]").toString()
        val result = JsonPath("$[2]..['name','id']").readFromJson<ArrayNode>(LARGE_JSON)
        assertEquals(expected, result!!.toString())
    }

    @Test
    fun shouldDeepScanListOfKeysOnDiffLevels() {
        val expected = readTree("[{\"name\":\"Marie Hampton\",\"company\":\"ZENCO\"},{\"name\":\"Felecia Bright\",\"id\":0},{\"name\":\"Maryanne Wiggins\"},{\"name\":\"Marylou Caldwell\",\"id\":2}]").toString()
        val result = JsonPath("$[2]..['name','company','id']").readFromJson<ArrayNode>(LARGE_JSON)
        assertEquals(expected, result!!.toString())
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
        val result = JsonPath("$[0]['latitude','longitude', 'isActive']").readFromJson<ObjectNode>(LARGE_JSON)
        val expected = createObjectNode().apply {
            put("latitude", -85.888651)
            put("longitude", 38.287152)
            put("isActive", true)
        }.toString()
        assertEquals(expected, result.toString())
    }

    @Test
    fun shouldBeNullIfPathDoesntExist() {
        assertNull(JsonPath("$.key").readFromJson<Int>("()"))
        assertNull(JsonPath("$.key").readFromJson<Int>("{}"))
        assertNull(JsonPath("$[0]").readFromJson<Int>("[]"))
        assertNull(JsonPath("$[0]").readFromJson<Int>(""))
        assertNull(JsonPath("$[2][0]").readFromJson<Int>("[]"))
    }
    
    @Test
    fun shouldBeNullIfRootMissingOrNullNode() {
        assertNull(JacksonUtil.mapper.missingNode().read("$"))
        assertNull(JacksonUtil.mapper.nullNode().read("$"))
    }

    @Test
    fun shouldBeAllItemsInList() {
        assertEquals(listOf("first", "second"), JsonPath("$[:]").readFromJson<List<String>>("""["first", "second"]"""))
        assertEquals(listOf("Thomas", "Mila", "Konstantin", "Tracy"), JsonPath("$..name[:]").readFromJson<List<String>>(FAMILY_JSON))
        assertEquals(listOf(
            mapOf(
                "name" to "Thomas",
                "age" to 13
            ),
            mapOf(
                "name" to "Mila",
                "age" to 18
            ),
            mapOf(
                "name" to "Konstantin",
                "age" to 29,
                "nickname" to "Kons"
            ),
            mapOf(
                "name" to "Tracy",
                "age" to 4
            )
        ), JsonPath("$.family.children[:]").readFromJson<List<Map<String, Any>>>(FAMILY_JSON))
    }

    // TODO implement $.* token
    @Test
    fun shouldPreserveOrder() {
        val result1 = JsonPath("$.store..price").readFromJson<List<Double>>(BOOKS_JSON)
        assertEquals(listOf(8.95, 12.99, 8.99, 22.99, 19.95), result1)


//        val json = """{"d": 4, "f": 6, "e": 5, "a": 1, "b": 2, "c": 3}"""
//        val jacksonResult = JacksonUtil.mapper.readTree(json).read<List<Int>>("$.*")
//        val jsonOrgResult = JSONObject(json).read<List<Int>>("$.*")
//        assertNotEquals(jacksonResult.toString(), jsonOrgResult.toString())
    }
}

data class ParsedResult(val outer: Map<String, Int>)
