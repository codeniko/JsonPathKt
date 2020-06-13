package com.nfeld.jsonpathlite

import com.fasterxml.jackson.databind.node.ArrayNode
import com.nfeld.jsonpathlite.util.createArrayNode
import com.nfeld.jsonpathlite.util.createObjectNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TokenTest : BaseNoCacheTest() {

    private fun printTesting(subpath: String) {
        println("Testing like $subpath")
    }

    @Test
    fun arrayAccessorToken() {
        assertNull(ArrayAccessorToken(0).read(createObjectNode()))
    }

    @Test
    fun multiArrayAccessorToken() {
        assertNull(MultiArrayAccessorToken(listOf(0)).read(createObjectNode()))

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

    @Test
    fun arrayLengthBasedRangeAccessorToken() {
        assertNull(ArrayLengthBasedRangeAccessorToken(0).read(createObjectNode()))
        assertEquals(createArrayNode().toString(), ArrayLengthBasedRangeAccessorToken(0, -1, 0).read(createArrayNode()).toString())
    }

    @Test
    fun arrayLengthBasedAccessorTokenToMultiArrayAccessorToken() {
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

    @Test
    fun deepScanLengthBasedArrayAccessorToken() {
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

    @Test
    fun objectAccessorToken() {
        assertNull(ObjectAccessorToken("key").read(createArrayNode()))
    }

    @Test
    fun multiObjectAccessorToken() {
        assertNull(MultiObjectAccessorToken(listOf()).read(createArrayNode()))
    }
}