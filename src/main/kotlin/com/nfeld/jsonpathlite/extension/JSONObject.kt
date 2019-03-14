package com.nfeld.jsonpathlite.extension

import com.nfeld.jsonpathlite.JsonPath
import org.json.JSONObject

fun <T : Any> JSONObject.read(jsonpath: String): T? {
    return JsonPath(jsonpath).readFromJson(this)
}

fun <T : Any> JSONObject.read(jsonpath: JsonPath): T? {
    return jsonpath.readFromJson(this)
}
