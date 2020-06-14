package com.nfeld.jsonpathlite

import com.nfeld.jsonpathlite.cache.CacheProvider
import io.kotest.core.spec.style.StringSpec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows

class PathCompilerTest : StringSpec({

    beforeTest {
        CacheProvider.setCache(null)
    }

    "compile" {
        val f = PathCompiler::compile

        assertEquals(listOf(
            ArrayAccessorToken(2),
            DeepScanObjectAccessorToken(listOf("name","id"))), f("$[2]..['name','id']"))
        assertEquals(listOf(
            ArrayAccessorToken(2),
            DeepScanObjectAccessorToken(listOf("name","id")),
            ArrayAccessorToken(2)), f("$[2]..['name','id'][2]"))

        assertEquals(listOf(DeepScanObjectAccessorToken(listOf("name"))), f("$..['name']"))
        assertEquals(listOf(DeepScanObjectAccessorToken(listOf("name","age"))), f("$..['name','age']"))
        assertEquals(listOf(DeepScanArrayAccessorToken(listOf(0))), f("$..[0]"))
        assertEquals(listOf(DeepScanArrayAccessorToken(listOf(0,1,6))), f("$..[0,1,6]"))
        assertEquals(listOf(DeepScanArrayAccessorToken(listOf(0,-1,-6))), f("$..[0,-1,-6]"))
        assertEquals(listOf(DeepScanArrayAccessorToken(listOf(-2))), f("$..[-2]"))
        assertEquals(listOf(DeepScanArrayAccessorToken(listOf(0,1,2))), f("$..[0:3]"))
        assertEquals(listOf(DeepScanArrayAccessorToken(listOf(0,1,2))), f("$..[:3]"))
        assertEquals(listOf(DeepScanLengthBasedArrayAccessorToken(1, null, 0)), f("$..[1:]"))
        assertEquals(listOf(DeepScanLengthBasedArrayAccessorToken(0, null, -2)), f("$..[:-2]"))
        assertEquals(listOf(DeepScanLengthBasedArrayAccessorToken(-5, null, 0)), f("$..[-5:]"))
        assertEquals(listOf(DeepScanLengthBasedArrayAccessorToken(0, null, 0)), f("$..[:]"))
        assertEquals(listOf(DeepScanLengthBasedArrayAccessorToken(0, null, -2)), f("$..[0:-2]"))
        assertEquals(listOf(DeepScanLengthBasedArrayAccessorToken(-5, 6, 0)), f("$..[-5:6]"))
        assertEquals(listOf(DeepScanLengthBasedArrayAccessorToken(-5, null, -2)), f("$..[-5:-2]"))
        assertEquals(listOf(ObjectAccessorToken("-")), f("$-"))
        assertEquals(listOf(ObjectAccessorToken("-0")), f("$-0"))
        assertEquals(listOf(ObjectAccessorToken("-"), ArrayAccessorToken(0)), f("$-[0]"))
        assertEquals(listOf(WildcardToken()), f("$.*"))
        assertEquals(listOf(WildcardToken(), ObjectAccessorToken("key")), f("$.*.key"))
        assertEquals(listOf(WildcardToken(), ArrayAccessorToken(3)), f("$.*[3]"))
        assertEquals(listOf(WildcardToken(), DeepScanObjectAccessorToken(listOf("key"))), f("$.*..key"))
        assertEquals(listOf(WildcardToken(), DeepScanArrayAccessorToken(listOf(1,2,3))), f("$.*..[1:4]"))
    }

    "findMatchingClosingBracket" {
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
        assertEquals(2, f("[:]", start))
        assertEquals(6, f("[0,1,2]", start))
        assertEquals(5, f("['a[']", start))
        assertEquals(5, f("['a]']", start))
        assertEquals(7, f("['a\\'b']", start))
        assertEquals(9, f("['a\\'\\']']", start))
        assertEquals(6, f("['4\\a']", start))
        assertEquals(2, f("[*]", start))
    }

    "compileBracket" {
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
        assertEquals(ArrayLengthBasedRangeAccessorToken(0, null, 0), f(findClosingIndex("$[:]"), start, end))
        assertEquals(MultiArrayAccessorToken(listOf(1,2,3)), f(findClosingIndex("$[1:4]"), start, end))
        assertEquals(MultiArrayAccessorToken(listOf(1,2,3)), f(findClosingIndex("$[1,2,3]"), start, end))
        assertEquals(MultiArrayAccessorToken(listOf(1,-2,3)), f(findClosingIndex("$[1,-2,3]"), start, end))
        assertEquals(ObjectAccessorToken("name"), f(findClosingIndex("$['name']"), start, end))
        assertEquals(ObjectAccessorToken("4"), f(findClosingIndex("$['4']"), start, end))
        assertEquals(MultiObjectAccessorToken(listOf("name", "age")), f(findClosingIndex("$['name','age']"), start, end))
        assertEquals(MultiObjectAccessorToken(listOf("name", "age", "4")), f(findClosingIndex("$['name','age',4]"), start, end))
        assertEquals(ObjectAccessorToken("name:age"), f(findClosingIndex("$['name:age']"), start, end))
        assertEquals(WildcardToken(), f(findClosingIndex("$[*]"), start, end))

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
        assertEquals(ArrayLengthBasedRangeAccessorToken(0, null, 0), f(findClosingIndex("$[ : ]"), start, end))
        assertEquals(ArrayLengthBasedRangeAccessorToken(2, null, 0), f(findClosingIndex("$[ 2 : ]"), start, end))
        assertEquals(MultiArrayAccessorToken(listOf(0,1)), f(findClosingIndex("$[  : 2 ]"), start, end))
        assertEquals(MultiArrayAccessorToken(listOf(1,2)), f(findClosingIndex("$[ 1 : 3 ]"), start, end))
        assertEquals(WildcardToken(), f(findClosingIndex("$[ *  ]"), start, end))
        assertEquals(ObjectAccessorToken("name"), f(findClosingIndex("$[  'name'  ]"), start, end))
    }

    "should throw" {
        val compile = PathCompiler::compile
        val compileBracket = PathCompiler::compileBracket

        assertThrows<IllegalArgumentException> { compile("[0]") } // needs $
        assertThrows<IllegalArgumentException> { compile("") } // needs $, cant be empty string
        assertThrows<IllegalArgumentException> { compile("$[]") } // needs value in brackets
        assertThrows<IllegalArgumentException> { compile("$['']") } // needs value in quotes
        assertThrows<IllegalArgumentException> { compile("$[") } // needs closing bracket
        assertThrows<IllegalArgumentException> { compile("$[[]") } // invalid char at end
        assertThrows<IllegalArgumentException> { compile("$[[]]") }
        assertThrows<IllegalArgumentException> { compile("$[[0]]") }
        assertThrows<IllegalArgumentException> { compile("$[0[0]]") }
        assertThrows<IllegalArgumentException> { compileBracket("$[]", 1, 2) } // no token returned
        assertThrows<IllegalArgumentException> { compile("$.") } // needs closing bracket
        assertThrows<IllegalArgumentException> { compile("$['\\") } // unexpected escape char
        assertThrows<IllegalArgumentException> { PathCompiler.findMatchingClosingBracket("$['4\\", 1) }
        assertThrows<IllegalArgumentException> { compile("$[-'0']") } // cant use both negative and object accessor
        assertThrows<IllegalArgumentException> { compile("$-[]") }
    }
})