package com.nfeld.jsonpathlite

import com.nfeld.jsonpathlite.extension.read
import org.json.JSONArray
import org.json.JSONObject

data class JsonObject(private val underlying: JSONObject) : JsonResult() {
    override fun <T : Any> read(path: String): T? = underlying.read(path)
}

data class JsonArray(private val underlying: JSONArray): JsonResult() {
    override fun <T : Any> read(path: String): T? = underlying.read(path)
}

sealed class JsonResult {
    abstract fun <T : Any> read(path: String): T?
}
