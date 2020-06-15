package com.nfeld.jsonpathlite

import com.fasterxml.jackson.databind.node.ArrayNode
import com.nfeld.jsonpathlite.cache.CacheProvider
import com.nfeld.jsonpathlite.util.createArrayNode
import com.nfeld.jsonpathlite.util.createObjectNode
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

private fun printTesting(subpath: String) {
    println("Testing like $subpath")
}

class TokenTest : StringSpec({
    beforeTest() {
        CacheProvider.setCache(null)
    }

    "ArrayAccessorToken" {
        assertNull(ArrayAccessorToken(0).read(createObjectNode()))
    }

    "MultiArrayAccessorToken" {
        MultiArrayAccessorToken(listOf(0,1)).read(createObjectNode())?.toString() shouldBe "[]"

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

    "ArrayLengthBasedRangeAccessorToken" {
        ArrayLengthBasedRangeAccessorToken(0).read(createObjectNode())?.toString() shouldBe "[]"
        assertEquals(createArrayNode().toString(), ArrayLengthBasedRangeAccessorToken(0, -1, 0).read(createArrayNode()).toString())
    }

    "ArrayLengthBasedAccessorToken to MultiArrayAccessorToken" {
        val json = readTree("[0,1,2,3,4]") as ArrayNode

        printTesting("[0:]")
        var res = ArrayLengthBasedRangeAccessorToken(0,null, 0).toMultiArrayAccessorToken(json)
        var expected = MultiArrayAccessorToken(listOf(0,1,2,3,4))
        assertEquals(expected, res)

        printTesting("[3:]")
        res = ArrayLengthBasedRangeAccessorToken(3,null,0).toMultiArrayAccessorToken(json)
        expected = MultiArrayAccessorToken(listOf(3,4))
        assertEquals(expected, res)

        printTesting("[:-1]")
        res = ArrayLengthBasedRangeAccessorToken(0,null, -1).toMultiArrayAccessorToken(json)
        expected = MultiArrayAccessorToken(listOf(0,1,2,3)) // this kind of range has end exclusive, so not really to end
        assertEquals(expected, res)

        // test starting edge
        printTesting("[:-4]")
        res = ArrayLengthBasedRangeAccessorToken(0,null, -4).toMultiArrayAccessorToken(json)
        expected = MultiArrayAccessorToken(listOf(0))
        assertEquals(expected, res)

        // test ending edge
        printTesting("[-1:]")
        res = ArrayLengthBasedRangeAccessorToken(-1,null, 0).toMultiArrayAccessorToken(json)
        expected = MultiArrayAccessorToken(listOf(4))
        assertEquals(expected, res)

        printTesting("[-2:]")
        res = ArrayLengthBasedRangeAccessorToken(-2,null, 0).toMultiArrayAccessorToken(json)
        expected = MultiArrayAccessorToken(listOf(3,4))
        assertEquals(expected, res)

        printTesting("[-4:-1]")
        res = ArrayLengthBasedRangeAccessorToken(-4,null, -1).toMultiArrayAccessorToken(json)
        expected = MultiArrayAccessorToken(listOf(1,2,3))
        assertEquals(expected, res)

        printTesting("[-4:4]")
        res = ArrayLengthBasedRangeAccessorToken(-4,4, 0).toMultiArrayAccessorToken(json)
        expected = MultiArrayAccessorToken(listOf(1,2,3))
        assertEquals(expected, res)

        printTesting("[2:-1]")
        res = ArrayLengthBasedRangeAccessorToken(2,null, -1).toMultiArrayAccessorToken(json)
        expected = MultiArrayAccessorToken(listOf(2,3))
        assertEquals(expected, res)

        printTesting("[:]")
        res = ArrayLengthBasedRangeAccessorToken(0, null, 0).toMultiArrayAccessorToken(json)
        expected = MultiArrayAccessorToken(listOf(0,1,2,3,4))
        assertEquals(expected, res)
    }

    "DeepScanLengthBasedArrayAccessorToken" {
        val json = readTree("[0,1,2,3,4]") as ArrayNode

        printTesting("[0:]")
        var res = DeepScanLengthBasedArrayAccessorToken(0,null, 0).read(json).toString()
        assertEquals(json.toString(), res)

        printTesting("[1:]")
        res = DeepScanLengthBasedArrayAccessorToken(1,null, 0).read(json).toString()
        assertEquals("[1,2,3,4]", res)

        printTesting("[:-2]")
        res = DeepScanLengthBasedArrayAccessorToken(0,null, -2).read(json).toString()
        assertEquals("[0,1,2]", res)

        printTesting("[-3:]")
        res = DeepScanLengthBasedArrayAccessorToken(-3,null, 0).read(json).toString()
        assertEquals("[2,3,4]", res)

        printTesting("[0:-2]")
        res = DeepScanLengthBasedArrayAccessorToken(0,null, -2).read(json).toString()
        assertEquals("[0,1,2]", res)

        printTesting("[-4:3]")
        res = DeepScanLengthBasedArrayAccessorToken(-4,3, 0).read(json).toString()
        assertEquals("[1,2]", res)

        printTesting("[-3:-1]")
        res = DeepScanLengthBasedArrayAccessorToken(-3,null, -1).read(json).toString()
        assertEquals("[2,3]", res)
    }

    "ObjectAccessorToken" {
        // object accessor on an array should consistently return list
        ObjectAccessorToken("key").read(createArrayNode()).toString() shouldBe "[]"
    }

    "MultiObjectAccessorToken" {
        // object accessor on an array should consistently return list
        MultiObjectAccessorToken(listOf("a", "b")).read(createArrayNode()).toString() shouldBe "[]"
    }

    "WildcardToken" {
        WildcardToken().read(createArrayNode()).toString() shouldBe """[]"""
        WildcardToken().read(createObjectNode()).toString() shouldBe """[]"""
        val arrayNode = readTree("""["string", 42, { "key": "value" }, [0, 1] ]""")
        val objectNode = readTree("""{ "some": "string", "int": 42, "object": { "key": "value" }, "array": [0, 1] }""")
        WildcardToken().read(arrayNode).toString() shouldBe """["string",42,{"key":"value"},[0,1]]"""
        WildcardToken().read(objectNode).toString() shouldBe """["string",42,{"key":"value"},[0,1]]"""
    }
})