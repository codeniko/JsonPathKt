package com.nfeld.jsonpathlite.extension

import com.nfeld.jsonpathlite.JsonPath
import org.json.JSONArray

fun <T : Any> JSONArray.read(jsonpath: String): T? {
    return JsonPath(jsonpath).readFromJson(this)
}

fun <T : Any> JSONArray.read(jsonpath: JsonPath): T? {
    return jsonpath.readFromJson(this)
}