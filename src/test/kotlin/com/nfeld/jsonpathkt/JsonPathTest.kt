package com.nfeld.jsonpathkt

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*
import com.nfeld.jsonpathkt.cache.CacheProvider
import com.nfeld.jsonpathkt.extension.read
import com.nfeld.jsonpathkt.util.JacksonUtil
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

data class PojoClass1(val key: String)
data class PojoClass2(val key: List<Any?>)
data class PojoClass3(val key: Map<String, Int>, val default: Int)

class JsonPathTest : DescribeSpec({
    beforeTest() {
        CacheProvider.setCache(null)
    }

    describe("Core parse tests") {
        it("should be null on parse failure") {
            JsonPath.parse("5{}") shouldBe null
            JsonPath.parse("5[]") shouldBe null
            JsonPath.parse("$") shouldBe null
            JsonPath.parse("{") shouldBe null
            JsonPath.parse("") shouldBe null
            JsonPath.parse("{]") shouldBe null
            JsonPath.parse("[}") shouldBe null
            JsonPath.parse("null") shouldBe null
            JsonPath.parse(null) shouldBe null
        }

        it("should be null if root node is Missing or Null") {
            JsonPath("$").readFromJson<Any>(JacksonUtil.mapper.missingNode()) shouldBe null
            JsonPath("$").readFromJson<Any>(JacksonUtil.mapper.nullNode()) shouldBe null
        }

        it("should be null when parsing root string but without quotes") {
            JsonPath.parse("hello") shouldBe null
        }

        it("should parse root string with quotes") {
            JsonPath.parse(""""hello"""") shouldBe TextNode("hello")
            JsonPath.parse(""""hello"""")!!.read<String>("$") shouldBe "hello"
        }

        it("should parse root values other than String") {
            JsonPath.parse("4") shouldBe IntNode(4)
            JsonPath.parse("4")!!.read<Int>("$") shouldBe 4
            JsonPath.parse("4.76") shouldBe DoubleNode(4.76)
            JsonPath.parse("4.76")!!.read<Double>("$") shouldBe 4.76
            JsonPath.parse("true") shouldBe BooleanNode.TRUE
            JsonPath.parse("true")!!.read<Boolean>("$") shouldBe true
            JsonPath.parse("false") shouldBe BooleanNode.FALSE
            JsonPath.parse("false")!!.read<Boolean>("$") shouldBe false
        }

        it("should be able to get ObjectNode") {
            JsonPath.parse(SMALL_JSON)!!.read<ObjectNode>("$") shouldBe readTree(SMALL_JSON)
        }

        it("should be able to get ArrayNode") {
            JsonPath.parse(SMALL_JSON_ARRAY)!!.read<ArrayNode>("$") shouldBe readTree(SMALL_JSON_ARRAY)
        }

        it("should be able to get inner ObjectNodes") {
            val json = """[{"outer": {"inner": 9} }]"""
            JsonPath.parse(json)!!.read<ObjectNode>("$[0]") shouldBe readTree(json)[0]
            JsonPath.parse(json)!!.read<ObjectNode>("$[0].outer") shouldBe readTree(json)[0]["outer"]
        }

        it("should get values deep in JSON") {
            JsonPath.parse(LARGE_JSON)!!.read<String>("$[0].friends[1].other.a.b['c']") shouldBe "yo"
            JsonPath.parse(LARGE_JSON)!!.read<String>("$[0].friends[-1]['name']") shouldBe "Harrell Pratt"
        }

        it("should preserve order") {
            JsonPath.parse(BOOKS_JSON)!!.read<List<Double>>("$.store..price") shouldBe listOf(8.95, 12.99, 8.99, 22.99, 19.95)
            JsonPath.parse("""{"d": 4, "f": 6, "e": 5, "a": 1, "b": 2, "c": 3}""")!!.read<List<Int>>("$.*") shouldBe listOf(4,6,5,1,2,3)
        }
    }

    describe("Type casting") {
        val json = """{
            "int": 5,
            "string": "hello",
            "boolean": true,
            "double": 6.35,
            "char": "a"
        }"""

        it("casts to Int") {
            JsonPath.parse(json)!!.read<Int>("$.int") shouldBe 5
            JsonPath.parse(json)!!.read<Int>("$.string") shouldBe null
            JsonPath.parse(json)!!.read<Int>("$.boolean") shouldBe null
            JsonPath.parse(json)!!.read<Int>("$.double") shouldBe 6
            JsonPath.parse(json)!!.read<Int>("$.char") shouldBe null
        }

        it("casts to Double") {
            JsonPath.parse(json)!!.read<Double>("$.int") shouldBe 5.0
            JsonPath.parse(json)!!.read<Double>("$.string") shouldBe null
            JsonPath.parse(json)!!.read<Double>("$.boolean") shouldBe null
            JsonPath.parse(json)!!.read<Double>("$.double") shouldBe 6.35
            JsonPath.parse(json)!!.read<Double>("$.char") shouldBe null
        }

        it("casts to Boolean") {
            JsonPath.parse(json)!!.read<Boolean>("$.int") shouldBe true
            JsonPath.parse(json)!!.read<Boolean>("$.string") shouldBe null
            JsonPath.parse(json)!!.read<Boolean>("$.boolean") shouldBe true
            JsonPath.parse(json)!!.read<Boolean>("$.double") shouldBe null
            JsonPath.parse(json)!!.read<Boolean>("$.char") shouldBe null
        }

        it("casts to Char") {
            JsonPath.parse(json)!!.read<Char>("$.int") shouldBe 5.toChar()
            JsonPath.parse(json)!!.read<Char>("$.string") shouldBe null
            JsonPath.parse(json)!!.read<Char>("$.boolean") shouldBe null
            JsonPath.parse(json)!!.read<Char>("$.double") shouldBe null
            JsonPath.parse(json)!!.read<Char>("$.char") shouldBe 'a'
        }

        it("everything can be cast to String") {
            JsonPath.parse(json)!!.read<String>("$.int") shouldBe "5"
            JsonPath.parse(json)!!.read<String>("$.string") shouldBe "hello"
            JsonPath.parse(json)!!.read<String>("$.boolean") shouldBe "true"
            JsonPath.parse(json)!!.read<String>("$.double") shouldBe "6.35"
            JsonPath.parse(json)!!.read<String>("$.char") shouldBe "a"
        }

        it("should cast POJO") {
            JsonPath.parse("""{"key": "value"}""")!!.read<PojoClass1>("$") shouldBe PojoClass1(key = "value")
            JsonPath.parse("""{"key": [1, "random", null, 1.765]}""")!!.read<PojoClass2>("$") shouldBe PojoClass2(key = listOf<Any?>(1, "random", null, 1.765))
            JsonPath.parse("""{"key": { "a": 1, "b": 2 }, "default": 3}""")!!.read<PojoClass3>("$") shouldBe PojoClass3(key = mapOf("a" to 1, "b" to 2), default = 3)
        }
    }

    describe("Object accessors") {
        it("should be null of key doesn't exist") {
            JsonPath.parse(SMALL_JSON)!!.read<Any>("$.unknownkey") shouldBe null
            JsonPath.parse(SMALL_JSON)!!.read<Any>("$['unknownkey']") shouldBe null
        }

        it("should get value if key exists") {
            JsonPath.parse(SMALL_JSON)!!.read<Int>("$.key") shouldBe 5
            JsonPath.parse(SMALL_JSON)!!.read<Int>("$['key']") shouldBe 5
        }

        it("should be null if reading null value") {
            JsonPath.parse("""{"key":null}""")!!.read<Int>("$['key']") shouldBe null
        }

        it("should access empty string key and other uncommon keys") {
            JsonPath.parse("""{"":4}""")!!.read<Int>("$['']") shouldBe 4
            JsonPath.parse("""{"":4}""")!!.read<Int>("$[\"\"]") shouldBe 4
            JsonPath.parse("""{"'":4}""")!!.read<Int>("$[\"'\"]") shouldBe 4
            JsonPath.parse("""{"'":4}""")!!.read<Int>("$['\\'']") shouldBe 4
            JsonPath.parse("""{"\"": 4}""")!!.read<Int>("""$["\""]""") shouldBe 4
            JsonPath.parse("""{"\"": 4}""")!!.read<Int>("""$['"']""") shouldBe 4
            JsonPath.parse("""{"\\": 4}""")!!.read<Int>("""$['\\']""") shouldBe 4
        }

        it("should read object keys that have numbers and/or symbols") {
            val key = "!@#\$%^&*()_-+=[]{}|:;<,>.?`~" // excluding '
            val json = """
                {
                    "key1": "a",
                    "ke2y": "b",
                    "ke3%y": "c",
                    "1234": "d",
                    "12$34": "e",
                    "abc{}3d": "f",
                    "$key": "g"
                }
            """
            JsonPath.parse(json)!!.read<String>("$.key1") shouldBe "a"
            JsonPath.parse(json)!!.read<String>("$['key1']") shouldBe "a"
            JsonPath.parse(json)!!.read<String>("$.ke2y") shouldBe "b"
            JsonPath.parse(json)!!.read<String>("$['ke2y']") shouldBe "b"
            JsonPath.parse(json)!!.read<String>("$.ke3%y") shouldBe "c"
            JsonPath.parse(json)!!.read<String>("$['ke3%y']") shouldBe "c"
            JsonPath.parse(json)!!.read<String>("$.1234") shouldBe "d"
            JsonPath.parse(json)!!.read<String>("$['1234']") shouldBe "d"
            JsonPath.parse(json)!!.read<String>("$.12$34") shouldBe "e"
            JsonPath.parse(json)!!.read<String>("$['12$34']") shouldBe "e"
            JsonPath.parse(json)!!.read<String>("$.abc{}3d") shouldBe "f"
            JsonPath.parse(json)!!.read<String>("$['abc{}3d']") shouldBe "f"
            JsonPath.parse(json)!!.read<String>("$['$key']") shouldBe "g"
        }

        it("should be null on unsupported selectors on objects") {
            JsonPath.parse(SMALL_JSON)!!.read<Int>("$[:]") shouldBe null
        }

        it("should read key from list if list item is an object") {
            JsonPath.parse("""[{"key": "ey"}, {"key": "bee"}, {"key": "see"}]""")!!.read<JsonNode>("$.key") shouldBe null
            JsonPath.parse("""[{"key": "ey"}, {"key": "bee"}, {"key": "see"}]""")!!.read<JsonNode>("$.*.key").toString() shouldBe """["ey","bee","see"]"""
            JsonPath.parse("""[{"key": "ey"}, {"key": "bee"}, {"key": "see"}]""")!!.read<JsonNode>("$[0,2].key").toString() shouldBe """["ey","see"]"""
            JsonPath.parse("""
                {
                    "one": {"key": "value"},
                    "two": {"k": "v"},
                    "three": {"some": "more", "key": "other value"}
                }
            """)!!.read<JsonNode>("$['one','three'].key").toString() shouldBe """["value","other value"]"""

            JsonPath.parse("""[{"a": 1},{"a": 1}]""")!!.read<JsonNode>("$[*].a").toString() shouldBe """[1,1]"""
            JsonPath.parse("""[{"a": 1},{"a": 1}]""")!!.read<JsonNode>("$.*.a").toString() shouldBe """[1,1]"""
        }

        describe("Multi object accessors") {
            it("should get list of scalars") {
                JsonPath.parse("""{"key": "value", "another": "entry"}""")!!.read<List<String>>("$['key','another']") shouldBe listOf("value", "entry")
            }

            it("should return empty list of reading from a list that's not root list") {
                JsonPath.parse("""[{"key": "value", "another": "entry"}]""")!!.read<Any>("$['key','another']").toString() shouldBe "[]"
                JsonPath.parse("""[{"key": "ey", "other": 1}, {"key": "bee"}, {"key": "see", "else": 3}]""")!!.read<JsonNode>("$['key','other']").toString() shouldBe "[]"
            }

            it("should read obj keys from root list") {
                JsonPath.parse("""[{"key": "value", "another": "entry"}]""")!!.read<List<String>>("$.*['key','another']") shouldBe listOf("value", "entry")
                JsonPath.parse("""[{"key": "ey", "other": 1}, {"key": "bee"}, {"key": "see", "else": 3}]""")!!.read<JsonNode>("$.*['key','other']").toString() shouldBe """["ey",1,"bee","see"]"""
            }

            it("should get all 3 keys") {
                JsonPath.parse(LARGE_JSON)!!.read<List<Any>>("$[0]['latitude','longitude','isActive']") shouldBe listOf(-85.888651, 38.287152, true)
            }

            it("should get only the key/value pairs when found") {
                JsonPath.parse(LARGE_JSON)!!.read<List<Double>>("$[0]['latitude','longitude', 'unknownkey']") shouldBe listOf( -85.888651, 38.287152)
            }
        }
    }

    describe("Array accessors") {
        it("should be null of index out of bounds") {
            JsonPath.parse(SMALL_JSON_ARRAY)!!.read<Any>("$[43]") shouldBe null
            JsonPath.parse(SMALL_JSON_ARRAY)!!.read<Any>("$[-43]") shouldBe null
        }

        it("should get value if value exists at index") {
            JsonPath.parse(SMALL_JSON_ARRAY)!!.read<Int>("$[2]") shouldBe 3
            JsonPath.parse(SMALL_JSON_ARRAY)!!.read<Int>("$[0]") shouldBe 1
        }

        it("should get value from ends") {
            JsonPath.parse(SMALL_JSON_ARRAY)!!.read<Int>("$[-2]") shouldBe 4
            JsonPath.parse(SMALL_JSON_ARRAY)!!.read<Int>("$[-4]") shouldBe 2
            JsonPath.parse(LARGE_JSON)!!.read<String>("$[0]['tags'][-1]") shouldBe "qui"
            JsonPath.parse(LARGE_JSON)!!.read<String>("$[0]['tags'][-3]") shouldBe "cillum"
        }

        it("-0 should get first item in array") {
            JsonPath.parse(SMALL_JSON_ARRAY)!!.read<Int>("$[-0]") shouldBe 1
        }

        it("should return null if used on JSON object") {
            JsonPath.parse("""{"key":3}""")!!.read<Any>("$[3]") shouldBe null
        }

        it("should return null if used on a scalar other than String") {
            JsonPath.parse("5")!!.read<Any>("$[0]") shouldBe null
            JsonPath.parse("5.34")!!.read<Any>("$[0]") shouldBe null
            JsonPath.parse("true")!!.read<Any>("$[0]") shouldBe null
            JsonPath.parse("false")!!.read<Any>("$[0]") shouldBe null
        }

        it("should get character at index if String scalar") {
            JsonPath.parse(""""hello"""")!!.read<String>("$[0]") shouldBe "h"
        }

        describe("Multi array accessors") {
            it("should get first, fourth, and sixth items") {
                JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$[0]['tags'][0,3,5]") shouldBe listOf("occaecat","labore","laboris")
            }

            it("should get only the items with valid index") {
                JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$[0]['tags'][0,30,50]") shouldBe listOf("occaecat")
            }

            it("should return empty list if used on JSON object") {
                JsonPath.parse("""{"key":3}""")!!.read<Any>("$[3,4]")?.toString() shouldBe "[]"
            }
        }

        describe("Array ranges") {
            it("should handle array range from start") {
                JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$[0]['tags'][:3]") shouldBe listOf("occaecat","mollit","ullamco")
                JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$[0]['tags'][:-4]") shouldBe listOf("occaecat","mollit","ullamco")
            }

            it("should handle array range to end") {
                JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$[0]['tags'][5:]") shouldBe listOf("laboris","qui")
                JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$[0]['tags'][-2:]") shouldBe listOf("laboris","qui")
            }

            it("should handle specified range, exclusive at end") {
                JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$[0]['tags'][3:5]") shouldBe listOf("labore","cillum")
                JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$[0]['tags'][3:-1]") shouldBe listOf("labore","cillum","laboris")
                JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$[0]['tags'][-6:4]") shouldBe listOf("mollit","ullamco","labore")
                JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$[0]['tags'][-3:-1]") shouldBe listOf("cillum","laboris")
            }

            it("should return range items up to end if end index out of bounds") {
                JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$[0]['tags'][5:30]") shouldBe listOf("laboris","qui")
            }

            it("should return range items up from start if start index out of bounds") {
                JsonPath.parse("""["first", "second", "third"]""")!!.read<List<String>>("$[-4:]") shouldBe listOf("first", "second", "third")
            }

            it("should return empty list if used on JSON object") {
                JsonPath.parse("""{"key":3}""")!!.read<Any>("$[1:3]")?.toString() shouldBe "[]"
            }

            it("should get all items in list") {
                JsonPath.parse("""["first", "second"]""")!!.read<List<String>>("$[:]") shouldBe listOf("first", "second")
                JsonPath.parse("""["first", "second"]""")!!.read<List<String>>("$[0:]") shouldBe listOf("first", "second")
                JsonPath.parse("""["first", "second"]""")!!.read<List<String>>("$") shouldBe listOf("first", "second")

                val expected = listOf(
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
                )
                JsonPath.parse(FAMILY_JSON)!!.read<List<Map<String, Any>>>("$.family.children[:]") shouldBe expected
                JsonPath.parse(FAMILY_JSON)!!.read<List<Map<String, Any>>>("$.family.children[0:]") shouldBe expected
            }
        }
    }

    describe("List collections") {
        it("should include nulls") {
            JsonPath.parse("""{"key": [1, "random", null, 1.765]}""")!!.read<List<Any?>>("$.key") shouldBe listOf(1, "random", null, 1.765)
        }

        it("should be String collection") {
            JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$[0].tags") shouldBe listOf("occaecat","mollit","ullamco","labore","cillum","laboris","qui")
        }

        it("should be Int collection") {
            JsonPath.parse(LARGE_JSON)!!.read<List<Int>>("$[5].nums") shouldBe listOf(1,2,3,4,5)
        }

        it("should be Long collection") {
            JsonPath.parse(LARGE_JSON)!!.read<List<Long>>("$[5].nums") shouldBe listOf(1L,2L,3L,4L,5L)
        }

        it("should get a Set collection to remove duplicates") {
            JsonPath.parse("""[1,2,3,1,2,4,5]""")!!.read<Set<Int>>("$") shouldBe setOf(1,2,3,4,5)
        }
    }

    describe("Map objects") {
        it("should be Map") {
            JsonPath.parse("""{"a": {"b": "yo"}}}""")!!.read<Map<String, Any>>("$") shouldBe mapOf("a" to mapOf("b" to "yo"))
            JsonPath.parse("""{"a": {"b": "yo"}}}""")!!.read<Map<String, String>>("$.a") shouldBe mapOf("b" to "yo")
        }
    }

    describe("Deep scans") {
        it("should get String list") {
            val expected = listOf("Salazar Casey","Kathrine Osborn","Vonda Howe","Harrell Pratt","Porter Cummings",
                "Mason Leach","Spencer Valenzuela","Hope Medina","Marie Hampton","Felecia Bright",
                "Maryanne Wiggins","Marylou Caldwell","Mari Pugh","Rios Norton","Judy Good","Rosetta Stanley",
                "Margret Quinn","Lora Cotton","Gaines Henry","Dorothea Irwin")
            JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$..name") shouldBe expected
            JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$..name") shouldBe expected
        }

        it("should get Double list") {
            JsonPath.parse(LARGE_JSON)!!.read<List<Double>>("$..latitude") shouldBe listOf(-85.888651, 71.831798, 78.266157, -10.214391, 32.293366)
            JsonPath.parse(LARGE_JSON)!!.read<List<Double>>("$..['latitude']") shouldBe listOf(-85.888651, 71.831798, 78.266157, -10.214391, 32.293366)
        }

        it("should get ArrayNode") {
            val expected = """[["occaecat","mollit","ullamco","labore","cillum","laboris","qui"],["aliquip","cillum","qui","ut","ea","eu","reprehenderit"],["nulla","elit","ipsum","pariatur","ullamco","ut","sint"],["fugiat","sit","ad","voluptate","officia","aute","duis"],["est","dolor","dolore","exercitation","minim","dolor","pariatur"]]"""
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$..tags")?.toString() shouldBe expected
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$..['tags']")?.toString() shouldBe expected
        }

        it("should get from longer path") {
            JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$[2]..name") shouldBe listOf("Marie Hampton", "Felecia Bright", "Maryanne Wiggins", "Marylou Caldwell")
            JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$[2]..['name']") shouldBe listOf("Marie Hampton", "Felecia Bright", "Maryanne Wiggins", "Marylou Caldwell")
        }

        it("should scan to get the first item of all sublists") {
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$[2]..[0]").toString() shouldBe """["nulla",{"id":0,"name":"Felecia Bright","other":{"a":{"b":{"c":"yo"}}}}]"""
        }

        it("should scan to get the last item of all sublists") {
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$[2]..[-1]").toString() shouldBe """["sint",{"id":2,"name":"Marylou Caldwell","other":{"a":{"b":{"c":"yo"}}}}]"""
        }

        it("should scan to get the first and third items of all sublists") {
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$[2]..[0,2]").toString() shouldBe """["nulla","ipsum",{"id":0,"name":"Felecia Bright","other":{"a":{"b":{"c":"yo"}}}},{"id":2,"name":"Marylou Caldwell","other":{"a":{"b":{"c":"yo"}}}}]"""
        }

        it("should scan to get the first and second from last items of all sublists") {
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$[2]..[0, -2]").toString() shouldBe """["nulla","ut",{"id":0,"name":"Felecia Bright","other":{"a":{"b":{"c":"yo"}}}},{"name":"Maryanne Wiggins","other":{"a":{"b":{"c":"yo"}}}}]"""
        }

        it("should scan to get the first and second (range) items of all sublists") {
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$[2]..[0:2]").toString() shouldBe """["nulla","elit",{"id":0,"name":"Felecia Bright","other":{"a":{"b":{"c":"yo"}}}},{"name":"Maryanne Wiggins","other":{"a":{"b":{"c":"yo"}}}}]"""
        }

        it("should scan to get the third and all following items of all sublists") {
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$[2]..[2:]").toString() shouldBe """["ipsum","pariatur","ullamco","ut","sint",{"id":2,"name":"Marylou Caldwell","other":{"a":{"b":{"c":"yo"}}}}]"""
        }

        it("should scan to get all items except for last item of all sublists") {
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$[2]..[:-1]").toString() shouldBe """["nulla","elit","ipsum","pariatur","ullamco","ut",{"id":0,"name":"Felecia Bright","other":{"a":{"b":{"c":"yo"}}}},{"name":"Maryanne Wiggins","other":{"a":{"b":{"c":"yo"}}}}]"""
        }

        it("should scan to get all items starting from second to last of all sublists") {
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$[2]..[-2:]").toString() shouldBe """["ut","sint",{"name":"Maryanne Wiggins","other":{"a":{"b":{"c":"yo"}}}},{"id":2,"name":"Marylou Caldwell","other":{"a":{"b":{"c":"yo"}}}}]"""
        }

        it("should scan to get all items between first and last of all sublists (both sides exclusive)") {
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$[2]..[1:-1]").toString() shouldBe """["elit","ipsum","pariatur","ullamco","ut",{"name":"Maryanne Wiggins","other":{"a":{"b":{"c":"yo"}}}}]"""
        }

        it("should scan to get all items from second to last, to 4th item of all sublists") {
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$[2]..[-2:5]").toString() shouldBe """[{"name":"Maryanne Wiggins","other":{"a":{"b":{"c":"yo"}}}},{"id":2,"name":"Marylou Caldwell","other":{"a":{"b":{"c":"yo"}}}}]"""
        }

        it("should scan to get second from last item of all sublists") {
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$[2]..[-2:-1]").toString() shouldBe """["ut",{"name":"Maryanne Wiggins","other":{"a":{"b":{"c":"yo"}}}}]"""
        }

        it("should scan to keys on same level of all sublists") {
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$[2]..['name','id']").toString() shouldBe """["Marie Hampton","Felecia Bright",0,"Maryanne Wiggins","Marylou Caldwell",2]"""
        }

        it("should scan to keys on different level of all sublists") {
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$[2]..['name','company','id']").toString() shouldBe """["Marie Hampton","ZENCO","Felecia Bright",0,"Maryanne Wiggins","Marylou Caldwell",2]"""
        }

        it("should scan to get all items after the first of all sublists even if end out of range") {
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$[2]..[1:100]").toString() shouldBe """["elit","ipsum","pariatur","ullamco","ut","sint",{"name":"Maryanne Wiggins","other":{"a":{"b":{"c":"yo"}}}},{"id":2,"name":"Marylou Caldwell","other":{"a":{"b":{"c":"yo"}}}}]"""
        }

        it("should get the key of every 2nd item in all sublists") {
            val json = """
                {
                  "k": [{"key": "some value"}, {"key": 42}],
                  "kk": [[{"key": 100}, {"key": 200}, {"key": 300}], [{"key": 400}, {"key": 500}, {"key": 600}]],
                  "key": [0, 1]
                }
            """.trimIndent()
            JsonPath.parse(json)!!.read<JsonNode>("$..[1].key").toString() shouldBe """[42,200,500]"""
        }
    }

    describe("Wildcard") {
        it("should handle all types of reads correctly") {
            // we have obj, list, list, and scalars. It should handle them all correctly the further we go down with wildcards
            // keep in mind wildcards always return a list, and drop scalars when going up a list. Scalars dont have levels to go up, only container nodes do

            JsonPath.parse("""[{"bar": [42]}]""")!!.read<JsonNode>("$.*").toString() shouldBe """[{"bar":[42]}]""" // in root list
            JsonPath.parse("""[{"bar": [42]}]""")!!.read<JsonNode>("$.*.*").toString() shouldBe """[[42]]""" // root list wrapped around item from next level
            JsonPath.parse("""[{"bar": [42]}]""")!!.read<JsonNode>("$.*.*.*").toString() shouldBe """[42]""" // root list wrapped around item from next level
            JsonPath.parse("""[{"bar": [42]}]""")!!.read<JsonNode>("$.*.*.*.*").toString() shouldBe "[]" // root list since wildcard always returns a list

            JsonPath.parse("""[{"bar": [42]},{"bae": [30]}]""")!!.read<JsonNode>("$.*").toString() shouldBe """[{"bar":[42]},{"bae":[30]}]""" // in root list
            JsonPath.parse("""[{"bar": [42]},{"bae": [30]}]""")!!.read<JsonNode>("$.*.*").toString() shouldBe """[[42],[30]]""" // root list wrapped around items from next level
            JsonPath.parse("""[{"bar": [42]},{"bae": [30]}]""")!!.read<JsonNode>("$.*.*.*").toString() shouldBe """[42,30]""" // root list wrapped around items from next level
            JsonPath.parse("""[{"bar": [42]},{"bae": [30]}]""")!!.read<JsonNode>("$.*.*.*.*").toString() shouldBe "[]" // root list since wildcard always returns a list

            JsonPath.parse("""[{"bar": [42]},{"bae": [30,31]}]""")!!.read<JsonNode>("$.*").toString() shouldBe """[{"bar":[42]},{"bae":[30,31]}]""" // in root list
            JsonPath.parse("""[{"bar": [42]},{"bae": [30,31]}]""")!!.read<JsonNode>("$.*.*").toString() shouldBe """[[42],[30,31]]""" // root list wrapped around items from next level
            JsonPath.parse("""[{"bar": [42]},{"bae": [30,31]}]""")!!.read<JsonNode>("$.*.*.*").toString() shouldBe """[42,30,31]""" // root list wrapped around items from next level
            JsonPath.parse("""[{"bar": [42]},{"bae": [30,31]}]""")!!.read<JsonNode>("$.*.*.*.*").toString() shouldBe "[]" // root list since wildcard always returns a list

            JsonPath.parse("""[{"bar": [42], "scalarkey": "scalar2"}, "scalar1"]""")!!.read<JsonNode>("$.*").toString() shouldBe """[{"bar":[42],"scalarkey":"scalar2"},"scalar1"]""" // in root list
            JsonPath.parse("""[{"bar": [42], "scalarkey": "scalar2"}, "scalar1"]""")!!.read<JsonNode>("$.*.*").toString() shouldBe """[[42],"scalar2"]""" // root list wrapped around items from next level
            JsonPath.parse("""[{"bar": [42], "scalarkey": "scalar2"}, "scalar1"]""")!!.read<JsonNode>("$.*.*.*").toString() shouldBe """[42]""" // root list wrapped around item from next level
            JsonPath.parse("""[{"bar": [42], "scalarkey": "scalar2"}, "scalar1"]""")!!.read<JsonNode>("$.*.*.*.*").toString() shouldBe "[]" // root list since wildcard always returns a list

            JsonPath.parse("""[[1], [2,3]]""")!!.read<JsonNode>("$.*").toString() shouldBe """[[1],[2,3]]"""
            JsonPath.parse("""[[1], [2,3]]""")!!.read<JsonNode>("$.*.*").toString() shouldBe """[1,2,3]"""
            JsonPath.parse("""[[1], [2,3]]""")!!.read<JsonNode>("$.*.*.*").toString() shouldBe """[]"""
        }

        it("should handle lists properly") {
            // Returns list held by "bar", wrapped in the root level list since wildcard always returns list
            JsonPath.parse("""[{"bar": [42]}]""")!!.read<JsonNode>("$[*].bar").toString() shouldBe """[[42]]"""
            // Returns root level list now with the item from the inner list.
            JsonPath.parse("""[{"bar": [42]}]""")!!.read<JsonNode>("$.*.bar.*").toString() shouldBe """[42]"""
            JsonPath.parse("""[{"bar": [42]}]""")!!.read<JsonNode>("$[*].bar[*]").toString() shouldBe """[42]"""
            // This would be a wildcard on a root level list which removes the scalars on that level. It should be list, not null as wildcard always returns list
            JsonPath.parse("""[{"bar": [42]}]""")!!.read<JsonNode>("$[*].bar[*][*]").toString() shouldBe """[]"""

            JsonPath.parse("""[[1], [2,3]]""")!!.read<JsonNode>("$.*[0]").toString() shouldBe """[1,2]""" // first item of each sublist in root
            JsonPath.parse("""[[1], [2,3]]""")!!.read<JsonNode>("$.*[1]").toString() shouldBe """[3]""" // second item of each sublist in root, which there is only 1 of
            JsonPath.parse("""[[1], [2,3]]""")!!.read<JsonNode>("$.*[1].*").toString() shouldBe """[]""" // second item of each sublist in root, which there is only 1 of

            println(JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$..friends").toString())
            println(JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$..friends..name").toString())
            println(JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$..friends..name[1]").toString())
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$..friends.*.name").toString() shouldBe """["Kathrine Osborn","Vonda Howe","Harrell Pratt","Mason Leach","Spencer Valenzuela","Hope Medina","Felecia Bright","Maryanne Wiggins","Marylou Caldwell","Rios Norton","Judy Good","Rosetta Stanley","Lora Cotton","Gaines Henry","Dorothea Irwin"]"""
            JsonPath.parse(LARGE_JSON)!!.read<List<String>>("$..friends.[*].name") shouldBe listOf("Kathrine Osborn","Vonda Howe","Harrell Pratt","Mason Leach","Spencer Valenzuela","Hope Medina","Felecia Bright","Maryanne Wiggins","Marylou Caldwell","Rios Norton","Judy Good","Rosetta Stanley","Lora Cotton","Gaines Henry","Dorothea Irwin")
        }

        it("should return null if null read before wildcard") {
            JsonPath.parse("{}")!!.read<Any>("$.key.*") shouldBe null
            JsonPath.parse("{}")!!.read<Any>("$.key[*]") shouldBe null
        }

        it("should return self if used on scalar") {
            JsonPath.parse("5")!!.read<Int>("$.*") shouldBe 5
            JsonPath.parse("5.34")!!.read<Double>("$.*") shouldBe 5.34
            JsonPath.parse("true")!!.read<Boolean>("$.*") shouldBe true
            JsonPath.parse("false")!!.read<Boolean>("$.*") shouldBe false
            JsonPath.parse(""""hello"""")!!.read<String>("$.*") shouldBe "hello"
        }

        it("should get the values of the JSON object") {
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$..friends[-1].*").toString() shouldBe """[2,"Harrell Pratt",{"a":{"b":{"c":"yo"}}},2,"Hope Medina",{"a":{"b":{"c":"yo"}}},2,"Marylou Caldwell",{"a":{"b":{"c":"yo"}}},2,"Rosetta Stanley",{"a":{"b":{"c":"yo"}}},2,"Dorothea Irwin",{"a":{"b":{"c":"yo"}}}]"""
            JsonPath.parse(LARGE_JSON)!!.read<JsonNode>("$..friends[-1][*]").toString() shouldBe """[2,"Harrell Pratt",{"a":{"b":{"c":"yo"}}},2,"Hope Medina",{"a":{"b":{"c":"yo"}}},2,"Marylou Caldwell",{"a":{"b":{"c":"yo"}}},2,"Rosetta Stanley",{"a":{"b":{"c":"yo"}}},2,"Dorothea Irwin",{"a":{"b":{"c":"yo"}}}]"""
        }
    }
})
