package com.nfeld.jsonpathlite

import org.json.JSONArray
import org.json.JSONObject

data class JsonObject(val underlying: JSONObject) : JsonResult() {
    override fun <T : Any> read(path: String): T? = JsonPath(path).readFromJson(underlying)
}

data class JsonArray(val underlying: JSONArray): JsonResult() {
    override fun <T : Any> read(path: String): T? = JsonPath(path).readFromJson(underlying)
}

sealed class JsonResult {
    abstract fun <T : Any> read(path: String): T?
}
