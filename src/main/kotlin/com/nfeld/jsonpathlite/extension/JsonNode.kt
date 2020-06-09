package com.nfeld.jsonpathlite.extension

import com.fasterxml.jackson.databind.JsonNode
import com.nfeld.jsonpathlite.JsonPath

inline fun <reified T : Any> JsonNode.read(jsonpath: String): T? {
    return JsonPath(jsonpath).readFromJson(this)
}

inline fun <reified T : Any> JsonNode.read(jsonpath: JsonPath): T? {
    return jsonpath.readFromJson(this)
}
