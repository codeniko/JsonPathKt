package com.nfeld.jsonpathkt.extension

import com.nfeld.jsonpathkt.JsonPath
import com.nfeld.jsonpathkt.SMALL_JSON
import com.nfeld.jsonpathkt.SMALL_JSON_ARRAY
import com.nfeld.jsonpathkt.readTree
import com.nfeld.jsonpathkt.util.RootLevelArrayNode
import com.nfeld.jsonpathkt.util.createArrayNode
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import org.junit.jupiter.api.Assertions.assertEquals

class JsonNodeTest : StringSpec({
    "should read root ArrayNode" {
        val jsonObj = readTree(SMALL_JSON_ARRAY)
        assertEquals(2, jsonObj.read(JsonPath("$[1]"))!!)
        assertEquals(2, jsonObj.read("$[1]")!!)
    }

    "should read root ObjectNode" {
        val jsonObj = readTree(SMALL_JSON)
        assertEquals(5, jsonObj.read(JsonPath("$['key']"))!!)
        assertEquals(5, jsonObj.read("$['key']")!!)
    }

    // we dont want to break any functionality if users expect to get a normal Jackson ArrayNode
    "RootLevelArrayNode should be equal to an ArrayNode if every way since we return it to users" {
        RootLevelArrayNode() shouldBe createArrayNode()
        createArrayNode() shouldBe RootLevelArrayNode()
    }
})