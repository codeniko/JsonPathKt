package com.nfeld.jsonpathlite

import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TokenTest : BaseTest() {

    private fun printTesting(subpath: String) {
        println("Testing like $subpath")
    }

    @Test
    fun arrayAccessorToken() {
        assertNull(ArrayAccessorToken(0).read(JSONObject()))
    }

    @Test
    fun multiArrayAccessorToken() {
        assertNull(MultiArrayAccessorToken(listOf(0)).read(JSONObject()))

        val expected = JSONArray().apply {
            put(1)
            put(3)
        }
        assertEquals(expected.toString(), MultiArrayAccessorToken(listOf(0, -1)).read(JSONArray().apply {
            put(1)
            put(2)
            put(3)
        }).toString())
    }

    @Test
    fun arrayLengthBasedRangeAccessorToken() {
        assertNull(ArrayLengthBasedRangeAccessorToken(0).read(JSONObject()))
        assertEquals(JSONArray().toString(), ArrayLengthBasedRangeAccessorToken(0, -1, 0).read(JSONArray()).toString())
    }

    @Test
    fun arrayLengthBasedAccessorTokenToMultiArrayAccessorToken() {
        val json = JSONArray("[0,1,2,3,4]")

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
    }

    @Test
    fun objectAccessorToken() {
        assertNull(ObjectAccessorToken("key").read(JSONArray()))
    }

    @Test
    fun multiObjectAccessorToken() {
        assertNull(MultiObjectAccessorToken(listOf()).read(JSONArray()))
    }
}