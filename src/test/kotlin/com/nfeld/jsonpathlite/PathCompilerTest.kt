package com.nfeld.jsonpathlite

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PathCompilerTest : BaseTest() {

    @Test
    fun findMatchingClosingBracket() {
        val start = 0
        val f = PathCompiler::findMatchingClosingBracket

        assertEquals(1, f("[]", start))
        assertEquals(2, f("[5]", start))
        assertEquals(3, f("[53]", start))
        assertEquals(4, f("['5']", start))
        assertEquals(3, f("[-5]", start))
        assertEquals(4, f("[-5:]", start))
        assertEquals(3, f("[:5]", start))
        assertEquals(4, f("[0:5]", start))
        assertEquals(6, f("[0,1,2]", start))
        assertEquals(5, f("['a[']", start))
        assertEquals(5, f("['a]']", start))
        assertEquals(7, f("['a\\'b']", start))
        assertEquals(9, f("['a\\'\\']']", start))
    }

    @Test
    fun compileBracket() {
        val f = PathCompiler::compileBracket
        val start = 1
        var end = 0

        fun findClosingIndex(path: String): String {
            println("Testing $path")
            end = PathCompiler.findMatchingClosingBracket(path, start)
            return path
        }

        assertEquals(ArrayAccessorToken(0), f(findClosingIndex("$[0]"), start, end))
        assertEquals(ArrayAccessorToken(-4), f(findClosingIndex("$[-4]"), start, end))
        assertEquals(MultiArrayAccessorToken(listOf(0,1,2)), f(findClosingIndex("$[:3]"), start, end))
        assertEquals(ArrayLengthBasedRangeAccessorToken(3, null,0), f(findClosingIndex("$[3:]"), start, end))
        assertEquals(MultiArrayAccessorToken(listOf(1,2,3)), f(findClosingIndex("$[1:4]"), start, end))
        assertEquals(MultiArrayAccessorToken(listOf(1,2,3)), f(findClosingIndex("$[1,2,3]"), start, end))
        assertEquals(MultiArrayAccessorToken(listOf(1,-2,3)), f(findClosingIndex("$[1,-2,3]"), start, end))
        assertEquals(ObjectAccessorToken("name"), f(findClosingIndex("$['name']"), start, end))
        assertEquals(ObjectAccessorToken("4"), f(findClosingIndex("$['4']"), start, end))
        assertEquals(MultiObjectAccessorToken(listOf("name", "age")), f(findClosingIndex("$['name','age']"), start, end))
        assertEquals(MultiObjectAccessorToken(listOf("name", "age", "4")), f(findClosingIndex("$['name','age',4]"), start, end))
        assertEquals(ObjectAccessorToken("name:age"), f(findClosingIndex("$['name:age']"), start, end))

        // handle negative values in array ranges
        assertEquals(ArrayLengthBasedRangeAccessorToken(0,null, -1), f(findClosingIndex("$[:-1]"), start, end))
        assertEquals(ArrayLengthBasedRangeAccessorToken(0,null, -3), f(findClosingIndex("$[:-3]"), start, end))
        assertEquals(ArrayLengthBasedRangeAccessorToken(-1,null, 0), f(findClosingIndex("$[-1:]"), start, end))
        assertEquals(ArrayLengthBasedRangeAccessorToken(-5,null, 0), f(findClosingIndex("$[-5:]"), start, end))
        assertEquals(ArrayLengthBasedRangeAccessorToken(-5,null, -1), f(findClosingIndex("$[-5:-1]"), start, end))
        assertEquals(ArrayLengthBasedRangeAccessorToken(5,null, -1), f(findClosingIndex("$[5:-1]"), start, end))
        assertEquals(ArrayLengthBasedRangeAccessorToken(-5,4, 0), f(findClosingIndex("$[-5:4]"), start, end))

        // ignore space paddings
        assertEquals(ArrayAccessorToken(0), f(findClosingIndex("$[  0  ]"), start, end))
        assertEquals(MultiArrayAccessorToken(listOf(0,3)), f(findClosingIndex("$[0,  3]"), start, end))
        assertEquals(ObjectAccessorToken("name"), f(findClosingIndex("$['name']"), start, end))
    }
}