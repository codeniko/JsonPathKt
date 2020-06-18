package com.nfeld.jsonpathkt.util

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class JacksonUtilTest : StringSpec({
    "should create ObjectNode" {
        createObjectNode() is ObjectNode
        createObjectNode().toString() shouldBe "{}"
    }

    "should create ArrayNode" {
        createArrayNode() is ArrayNode
        createArrayNode().toString() shouldBe "[]"
    }
})