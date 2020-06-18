package com.nfeld.jsonpathkt

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.TextNode
import com.nfeld.jsonpathkt.cache.CacheProvider
import com.nfeld.jsonpathkt.util.RootLevelArrayNode
import com.nfeld.jsonpathkt.util.createArrayNode
import com.nfeld.jsonpathkt.util.createObjectNode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.assertEquals

private fun printTesting(subpath: String) {
    println("Testing like $subpath")
}

class TokenTest : DescribeSpec({
    beforeTest() {
        CacheProvider.setCache(null)
    }

    describe("Token tests") {
        describe("ArrayAccessorToken") {
            it("should be null if item doesnt exist at index") {
                ArrayAccessorToken(0).read(createObjectNode()) shouldBe null
            }

            it("should get the item if it exists at index") {
                ArrayAccessorToken(0).read(readTree("[1,2]")).toString() shouldBe "1"
            }

            it("should get the item if it exists at index if negative") {
                ArrayAccessorToken(-1).read(readTree("[1,2]")).toString() shouldBe "2"
            }

            it("should get last item") {
                ArrayAccessorToken(-1).read(readTree("[1,2]")).toString() shouldBe "2"
            }

            it("should be null if node is an ObjectNode") {
                ArrayAccessorToken(0).read(readTree("""{"0":1}""")) shouldBe null
            }

            it("should get item if node is a RootLevelArrayNode") {
                val rootJson = readTree("[[1]]") as ArrayNode
                ArrayAccessorToken(0).read(RootLevelArrayNode(rootJson)).toString() shouldBe "[1]" // list since it was root level
            }

            it("should get first item of sublists if node is a RootLevelArrayNode") {
                val rootJson = readTree("[1,[2],[3,4],[5,6,7]]") as ArrayNode
                ArrayAccessorToken(0).read(RootLevelArrayNode(rootJson)).toString() shouldBe "[2,3,5]"
            }

            it("should get last item of sublists if node is a RootLevelArrayNode") {
                val rootJson = readTree("[1,[2],[3,4],[5,6,7]]") as ArrayNode
                ArrayAccessorToken(-1).read(RootLevelArrayNode(rootJson)).toString() shouldBe "[2,4,7]"
            }

            it("should get character of a String at specified index") {
                ArrayAccessorToken(1).read(readTree("\"hello\"")).toString() shouldBe "\"e\""
                ArrayAccessorToken(-1).read(readTree("\"hello\"")).toString() shouldBe "\"o\""
                ArrayAccessorToken(-8).read(readTree("\"hello\"")) shouldBe null // out of bounds
            }

            it("should get specified character of every String in RootLevelArrayNode") {
                ArrayAccessorToken(1).read(WildcardToken().read(readTree("""["hello","world"]"""))!!).toString() shouldBe """["e","o"]"""
                ArrayAccessorToken(-1).read(WildcardToken().read(readTree("""["hello","world"]"""))!!).toString() shouldBe """["o","d"]"""
                ArrayAccessorToken(-4).read(WildcardToken().read(readTree("""["h","world"]"""))!!).toString() shouldBe """["o"]"""
            }
        }

        describe("MultiArrayAccessorToken") {
            it("should get items at specified indices") {
                MultiArrayAccessorToken(listOf(0, 1)).read(createObjectNode())?.toString() shouldBe "[]"

                val expected = createArrayNode().apply {
                    add(1)
                    add(3)
                }
                assertEquals(expected.toString(), MultiArrayAccessorToken(listOf(0, -1)).read(createArrayNode().apply {
                    add(1)
                    add(2)
                    add(3)
                }).toString())
            }

            it("should get specified items of sublists if node is a RootLevelArrayNode") {
                val json = readTree("[1,[2],[3,4],[5,6,7]]") as ArrayNode
                MultiArrayAccessorToken(listOf(0, 1)).read(RootLevelArrayNode(json)).toString() shouldBe "[2,3,4,5,6]"
                MultiArrayAccessorToken(listOf(0, -1)).read(RootLevelArrayNode(json)).toString() shouldBe "[2,2,3,4,5,7]"
            }

            it("should be able to get same index multiple times") {
                val json = readTree("[1,[2],[3,4],[5,6,7]]") as ArrayNode
                MultiArrayAccessorToken(listOf(0, 0, 0)).read(json).toString() shouldBe "[1,1,1]"
                MultiArrayAccessorToken(listOf(2, 2)).read(json).toString() shouldBe "[[3,4],[3,4]]"
                MultiArrayAccessorToken(listOf(0, 0)).read(RootLevelArrayNode(json)).toString() shouldBe "[2,2,3,3,5,5]"
            }

            it("should get characters of a String at specified indices") {
                MultiArrayAccessorToken(listOf(1,4)).read(readTree("\"hello\"")).toString() shouldBe """["e","o"]"""
            }

            it("should get specified characters of every String in RootLevelArrayNode") {
                MultiArrayAccessorToken(listOf(0,1)).read(WildcardToken().read(readTree("""["hello","world"]"""))!!).toString() shouldBe """["h","e","w","o"]"""
            }
        }

        describe("ArrayLengthBasedRangeAccessorToken") {
            it("should return empty list") {
                ArrayLengthBasedRangeAccessorToken(0).read(createObjectNode())?.toString() shouldBe "[]"
            }

            it("should not get characters of a String") {
                ArrayLengthBasedRangeAccessorToken(1).read(readTree("\"hello\"")).toString() shouldBe "[]"
            }

            it("should not get characters of every String in RootLevelArrayNode") {
                ArrayLengthBasedRangeAccessorToken(0,2).read(WildcardToken().read(readTree("""["hello","world"]"""))!!).toString() shouldBe "[]"
                ArrayLengthBasedRangeAccessorToken(2,null, -1).read(WildcardToken().read(readTree("""["hello","world"]"""))!!).toString() shouldBe "[]"
            }

            it("should handle objects in RootLevelArrayNode") {
                ArrayLengthBasedRangeAccessorToken(0, 1).read(WildcardToken().read(readTree("""[{"a":1,"b":{"c":2,"d":3},"e":4}]"""))!!).toString() shouldBe "[]"
                ArrayLengthBasedRangeAccessorToken(0, -1).read(WildcardToken().read(readTree("""[{"a":1,"b":{"c":2,"d":3},"e":4}]"""))!!).toString() shouldBe "[]"
                ArrayLengthBasedRangeAccessorToken(0, -1).read(WildcardToken().read(readTree("""[{"p":true},{"a":1,"b":{"c":2,"d":3},"e":4}]"""))!!).toString() shouldBe "[]"
            }

            it("should handle different levels of list nesting") {
                ArrayLengthBasedRangeAccessorToken(0, null, -1).read(readTree("""[1,[2],[3,4],[5,6,7]]""")).toString() shouldBe "[1,[2],[3,4]]"
                ArrayLengthBasedRangeAccessorToken(0, null, 0).read(readTree("""[1,[2],[3,4],[5,6,7]]""")).toString() shouldBe "[1,[2],[3,4],[5,6,7]]"
                ArrayLengthBasedRangeAccessorToken(0).read(WildcardToken().read(readTree("""[1,[2],[3,4],[5,6,7]]"""))!!).toString() shouldBe "[2,3,4,5,6,7]"
                ArrayLengthBasedRangeAccessorToken(0, null, -1).read(WildcardToken().read(readTree("""[1,[2],[3,4],[5,6,7]]"""))!!).toString() shouldBe "[3,5,6]"
                ArrayLengthBasedRangeAccessorToken(0, null, 0).read(WildcardToken().read(readTree("""[1,[2],[3,4],[5,6,7,[8,9,10,11]]]"""))!!).toString() shouldBe "[2,3,4,5,6,7,[8,9,10,11]]"
            }

            it("to MultiArrayAccessorToken general cases") {
                val json = readTree("[0,1,2,3,4]") as ArrayNode

                printTesting("[0:]")
                var res = ArrayLengthBasedRangeAccessorToken(0, null, 0).toMultiArrayAccessorToken(json)
                var expected = MultiArrayAccessorToken(listOf(0, 1, 2, 3, 4))
                assertEquals(expected, res)

                printTesting("[3:]")
                res = ArrayLengthBasedRangeAccessorToken(3, null, 0).toMultiArrayAccessorToken(json)
                expected = MultiArrayAccessorToken(listOf(3, 4))
                assertEquals(expected, res)

                printTesting("[:-1]")
                res = ArrayLengthBasedRangeAccessorToken(0, null, -1).toMultiArrayAccessorToken(json)
                expected = MultiArrayAccessorToken(
                    listOf(
                        0,
                        1,
                        2,
                        3
                    )
                ) // this kind of range has end exclusive, so not really to end
                assertEquals(expected, res)

                // test starting edge
                printTesting("[:-4]")
                res = ArrayLengthBasedRangeAccessorToken(0, null, -4).toMultiArrayAccessorToken(json)
                expected = MultiArrayAccessorToken(listOf(0))
                assertEquals(expected, res)

                // test ending edge
                printTesting("[-1:]")
                res = ArrayLengthBasedRangeAccessorToken(-1, null, 0).toMultiArrayAccessorToken(json)
                expected = MultiArrayAccessorToken(listOf(4))
                assertEquals(expected, res)

                printTesting("[-2:]")
                res = ArrayLengthBasedRangeAccessorToken(-2, null, 0).toMultiArrayAccessorToken(json)
                expected = MultiArrayAccessorToken(listOf(3, 4))
                assertEquals(expected, res)

                printTesting("[-4:-1]")
                res = ArrayLengthBasedRangeAccessorToken(-4, null, -1).toMultiArrayAccessorToken(json)
                expected = MultiArrayAccessorToken(listOf(1, 2, 3))
                assertEquals(expected, res)

                printTesting("[-4:4]")
                res = ArrayLengthBasedRangeAccessorToken(-4, 4, 0).toMultiArrayAccessorToken(json)
                expected = MultiArrayAccessorToken(listOf(1, 2, 3))
                assertEquals(expected, res)

                printTesting("[2:-1]")
                res = ArrayLengthBasedRangeAccessorToken(2, null, -1).toMultiArrayAccessorToken(json)
                expected = MultiArrayAccessorToken(listOf(2, 3))
                assertEquals(expected, res)

                printTesting("[:]")
                res = ArrayLengthBasedRangeAccessorToken(0, null, 0).toMultiArrayAccessorToken(json)
                expected = MultiArrayAccessorToken(listOf(0, 1, 2, 3, 4))
                assertEquals(expected, res)
            }
        }

        describe("DeepScanLengthBasedArrayAccessorToken") {
            it("should handle general cases") {
                val json = readTree("[0,1,2,3,4]") as ArrayNode

                printTesting("[0:]")
                var res = DeepScanLengthBasedArrayAccessorToken(0, null, 0).read(json).toString()
                assertEquals(json.toString(), res)

                printTesting("[1:]")
                res = DeepScanLengthBasedArrayAccessorToken(1, null, 0).read(json).toString()
                assertEquals("[1,2,3,4]", res)

                printTesting("[:-2]")
                res = DeepScanLengthBasedArrayAccessorToken(0, null, -2).read(json).toString()
                assertEquals("[0,1,2]", res)

                printTesting("[-3:]")
                res = DeepScanLengthBasedArrayAccessorToken(-3, null, 0).read(json).toString()
                assertEquals("[2,3,4]", res)

                printTesting("[0:-2]")
                res = DeepScanLengthBasedArrayAccessorToken(0, null, -2).read(json).toString()
                assertEquals("[0,1,2]", res)

                printTesting("[-4:3]")
                res = DeepScanLengthBasedArrayAccessorToken(-4, 3, 0).read(json).toString()
                assertEquals("[1,2]", res)

                printTesting("[-3:-1]")
                res = DeepScanLengthBasedArrayAccessorToken(-3, null, -1).read(json).toString()
                assertEquals("[2,3]", res)
            }

            it("should handle different levels of list nesting") {
                DeepScanLengthBasedArrayAccessorToken(0, null, 0).read(readTree("""[1,[2],[3,4],[5,6,7]]""")).toString() shouldBe "[1,[2],[3,4],[5,6,7],2,3,4,5,6,7]"
                DeepScanLengthBasedArrayAccessorToken(0, null, -1).read(readTree("""[1,[2],[3,4],[5,6,7]]""")).toString() shouldBe "[1,[2],[3,4],3,5,6]"
                DeepScanLengthBasedArrayAccessorToken(0, null, 0).read(WildcardToken().read(readTree("""[1,[2],[3,4],[5,6,7]]"""))!!).toString() shouldBe "[2,3,4,5,6,7]"
                DeepScanLengthBasedArrayAccessorToken(0, null, -1).read(WildcardToken().read(readTree("""[1,[2],[3,4],[5,6,7]]"""))!!).toString() shouldBe "[3,5,6]"
                DeepScanLengthBasedArrayAccessorToken(0, null, 0).read(WildcardToken().read(readTree("""[1,[2],[3,4],[5,6,7,[8,9,10,11]]]"""))!!).toString() shouldBe "[2,3,4,5,6,7,[8,9,10,11],8,9,10,11]"
            }
        }

        describe("ObjectAccessorToken") {
            val objJson = readTree("""{"key":1}""")!!
            val arrJson = readTree("""[{"key":1}]""")!!
            // object accessor on an array should return list

            it("should get value from key if it exists") {
                ObjectAccessorToken("key").read(objJson).toString() shouldBe "1"
            }

            it("should be null if key doesnt exist") {
                ObjectAccessorToken("missing").read(objJson) shouldBe null
            }

            it("should be null if node is an ArrayNode") {
                ObjectAccessorToken("key").read(arrJson) shouldBe null
            }

            it("should get value from key if node is a RootLevelArrayNode") {
                val rootJson = WildcardToken().read(arrJson)!! // should not be null
                ObjectAccessorToken("key").read(rootJson).toString() shouldBe "[1]" // list since it was root level
            }
        }

        describe("MultiObjectAccessorToken") {
            it("should return empty if accessing non root array or scalars") {
                // object accessor on an array should consistently return list
                MultiObjectAccessorToken(listOf("a", "b")).read(createArrayNode()).toString() shouldBe "[]"
                MultiObjectAccessorToken(listOf("a", "b")).read(TextNode("yo")).toString() shouldBe "[]"
                MultiObjectAccessorToken(listOf("a", "b")).read(BooleanNode.TRUE).toString() shouldBe "[]"
            }

            it("should get the values if they exist in object") {
                MultiObjectAccessorToken(listOf("a", "b")).read(readTree("""{"a":1,"b":2,"c":3}""")).toString() shouldBe "[1,2]"
                MultiObjectAccessorToken(listOf("a", "b")).read(readTree("""{"a":1,"b":2}""")).toString() shouldBe "[1,2]"
                MultiObjectAccessorToken(listOf("a", "b")).read(readTree("""{"a":1}""")).toString() shouldBe "[1]"
            }

            it("should get the values from subcontainers in root array node") {
                MultiObjectAccessorToken(listOf("a", "b")).read(RootLevelArrayNode(readTree("""[{"a":1,"b":2,"c":3}]""") as ArrayNode)).toString() shouldBe "[1,2]"
                MultiObjectAccessorToken(listOf("a", "b")).read(RootLevelArrayNode(readTree("""[{"a":1,"b":2}]""") as ArrayNode)).toString() shouldBe "[1,2]"
                MultiObjectAccessorToken(listOf("a", "b")).read(RootLevelArrayNode(readTree("""[{"a":1}]""") as ArrayNode)).toString() shouldBe "[1]"
                MultiObjectAccessorToken(listOf("a", "b")).read(RootLevelArrayNode(readTree("""[{}]""") as ArrayNode)).toString() shouldBe "[]"
                MultiObjectAccessorToken(listOf("a", "b")).read(RootLevelArrayNode(readTree("""[]""") as ArrayNode)).toString() shouldBe "[]"
            }
        }

        describe("DeepScanObjectAccessorToken") {
            it("should scan for keys") {
                DeepScanObjectAccessorToken(listOf("name")).read(readTree(FAMILY_JSON)).toString() shouldBe """["Thomas","Mila","Konstantin","Tracy"]"""
                DeepScanObjectAccessorToken(listOf("nickname")).read(readTree(FAMILY_JSON)).toString() shouldBe """["Kons"]"""
                DeepScanObjectAccessorToken(listOf("name","age")).read(readTree(FAMILY_JSON)).toString() shouldBe """["Thomas",13,"Mila",18,"Konstantin",29,"Tracy",4]"""
                DeepScanObjectAccessorToken(listOf("name","nickname")).read(readTree(FAMILY_JSON)).toString() shouldBe """["Thomas","Mila","Konstantin","Kons","Tracy"]"""
            }

            it("should place scan results into a RootLevelArrayNode") {
                (DeepScanObjectAccessorToken(listOf("name")).read(readTree(FAMILY_JSON)) is RootLevelArrayNode) shouldBe true
            }

            it("should handle objects on different levels") {
                val json = readTree("""[{"a":1},{"a":2,"b":3},{"a":4,"b":5,"c":{"a":6,"b":7,"c":8}}]""")
                DeepScanObjectAccessorToken(listOf("a")).read(json).toString() shouldBe """[1,2,4,6]"""
                DeepScanObjectAccessorToken(listOf("c")).read(json).toString() shouldBe """[{"a":6,"b":7,"c":8},8]"""
                DeepScanObjectAccessorToken(listOf("a","c")).read(json).toString() shouldBe """[1,2,4,{"a":6,"b":7,"c":8},6,8]"""
                DeepScanObjectAccessorToken(listOf("a")).read(WildcardToken().read(json)!!).toString() shouldBe """[1,2,4,6]"""
                DeepScanObjectAccessorToken(listOf("c")).read(WildcardToken().read(json)!!).toString() shouldBe """[{"a":6,"b":7,"c":8},8]"""
                DeepScanObjectAccessorToken(listOf("a","c")).read(WildcardToken().read(json)!!).toString() shouldBe """[1,2,4,{"a":6,"b":7,"c":8},6,8]"""
            }
        }

        describe("DeepScanArrayAccessorToken") {
            it("should scan for indices") {
                DeepScanArrayAccessorToken(listOf(0)).read(readTree(FAMILY_JSON)).toString() shouldBe """[{"name":"Thomas","age":13}]"""
                DeepScanArrayAccessorToken(listOf(0,2)).read(readTree(FAMILY_JSON)).toString() shouldBe """[{"name":"Thomas","age":13},{"name":"Konstantin","age":29,"nickname":"Kons"}]"""
            }

            it("should place scan results into a RootLevelArrayNode") {
                (DeepScanArrayAccessorToken(listOf(0)).read(readTree(FAMILY_JSON)) is RootLevelArrayNode) shouldBe true
            }

            it("should handle different nested lists") {
                val json = readTree("""[ {"a":1}, {"b":2}, [0,1,2, [ true, false ]] ]""")
                DeepScanArrayAccessorToken(listOf(0)).read(json).toString() shouldBe """[{"a":1},0,true]"""
                DeepScanArrayAccessorToken(listOf(0,1)).read(json).toString() shouldBe """[{"a":1},{"b":2},0,1,true,false]"""

                DeepScanArrayAccessorToken(listOf(0)).read(readTree("""[1,[2],[3,4],[5,6,7]]""")).toString() shouldBe "[1,2,3,5]"
                DeepScanArrayAccessorToken(listOf(0, 1)).read(readTree("""[1,[2],[3,4],[5,6,7]]""")).toString() shouldBe "[1,[2],2,3,4,5,6]"
                DeepScanArrayAccessorToken(listOf(0, -1)).read(readTree("""[1,[2],[3,4],[5,6,7]]""")).toString() shouldBe "[1,[5,6,7],2,2,3,4,5,7]"
                DeepScanArrayAccessorToken(listOf(0)).read(WildcardToken().read(readTree("""[1,[2],[3,4],[5,6,7]]"""))!!).toString() shouldBe "[2,3,5]"
                DeepScanArrayAccessorToken(listOf(0, 1)).read(WildcardToken().read(readTree("""[1,[2],[3,4],[5,6,7]]"""))!!).toString() shouldBe "[2,3,4,5,6]"
                DeepScanArrayAccessorToken(listOf(0, -1)).read(WildcardToken().read(readTree("""[1,[2],[3,4],[5,6,7]]"""))!!).toString() shouldBe "[2,2,3,4,5,7]"
                DeepScanArrayAccessorToken(listOf(0, 1)).read(WildcardToken().read(readTree("""[1,[2],[3,4],[5,6,7,[8,9,10,11]]]"""))!!).toString() shouldBe "[2,3,4,5,6,8,9]"
                DeepScanArrayAccessorToken(listOf(0, -1)).read(WildcardToken().read(readTree("""[1,[2],[3,4],[5,6,7,[8,9,10,11]]]"""))!!).toString() shouldBe "[2,2,3,4,5,[8,9,10,11],8,11]"
            }
        }

        describe("WildcardToken") {
            it("should handle empty cases") {
                WildcardToken().read(createArrayNode()).toString() shouldBe """[]"""
                WildcardToken().read(createObjectNode()).toString() shouldBe """[]"""
            }

            it("should get values from objects and strip") {
                val objectNode = readTree("""{ "some": "string", "int": 42, "object": { "key": "value" }, "array": [0, 1] }""")
                WildcardToken().read(objectNode).toString() shouldBe """["string",42,{"key":"value"},[0,1]]"""
            }

            it("should return a RootLevelArrayNode if root list replaced with another list before modifying values") {
                val arrayNode = readTree("""["string", 42, { "key": "value" }, [0, 1] ]""")
                WildcardToken().read(arrayNode).toString() shouldBe """["string",42,{"key":"value"},[0,1]]"""
            }

            it("should drop scalars and move everything down on root RootLevelArrayNode") {
                val arrayNode = readTree("""["string", 42, { "key": "value" }, [0, 1] ]""")
                val res1 = WildcardToken().read(arrayNode)
                (res1 is RootLevelArrayNode) shouldBe true
                val res2 = WildcardToken().read(res1!!)
                res2.toString() shouldBe """["value",0,1]"""
            }

            it("should override toString, hashCode, and equals") {
                WildcardToken().toString() shouldBe "WildcardToken"
                WildcardToken().hashCode() shouldBe "WildcardToken".hashCode()
                WildcardToken().equals(WildcardToken()) shouldBe true
                WildcardToken().equals(RootLevelArrayNode()) shouldBe false
            }
        }
    }
})