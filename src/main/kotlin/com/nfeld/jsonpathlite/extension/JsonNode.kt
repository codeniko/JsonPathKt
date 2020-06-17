package com.nfeld.jsonpathlite.extension

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.nfeld.jsonpathlite.JsonPath

inline fun <reified T : Any> JsonNode.read(jsonpath: String): T? {
    return JsonPath(jsonpath).readFromJson(this)
}

inline fun <reified T : Any> JsonNode.read(jsonpath: JsonPath): T? {
    return jsonpath.readFromJson(this)
}

internal inline fun JsonNode?.isNotNullOrMissing(): Boolean {
    return this != null && this !is NullNode && this !is MissingNode
}

internal inline fun ArrayNode.children(): List<JsonNode> {
    return map { it }
}

internal inline fun ObjectNode.getValueIfNotNullOrMissing(key: String): JsonNode? {
    val value = get(key)
    return if (value.isNotNullOrMissing()) {
        value
    } else null
}

internal inline fun ArrayNode.getValueIfNotNullOrMissing(index: Int): JsonNode? {
    val value = get(index)
    return if (value.isNotNullOrMissing()) {
        value
    } else null
}