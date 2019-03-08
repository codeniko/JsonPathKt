package com.nfeld.jsonpathlite

import org.json.JSONArray
import org.json.JSONObject

internal data class ArrayAccessorToken(val index: Int, val fromLast: Boolean) : Token {
    override fun read(json: Any): Any? {
        if (json is JSONArray) {
            if (fromLast && index > 0) {
                // optimized to get array length only if we're accessing from last
                val indexFromLast = json.length() - index
                if (indexFromLast >= 0) {
                    return json.opt(indexFromLast)
                }
            }
            return json.opt(index)
        }
        return null
    }
}

internal data class ObjectAccessorToken(val key: String) : Token {
    override fun read(json: Any): Any? {
        return if (json is JSONObject) {
            json.opt(key)
        } else null
    }
}

internal data class DeepScanToken(val targetKey: String) : Token {
    private val results = JSONArray()

    private fun scan(jsonValue: Any) {
        when (jsonValue) {
            is JSONObject -> {
                jsonValue.keySet().forEach { objKey ->
                    val objValue = jsonValue.opt(objKey)
                    if (objKey == targetKey) {
                        results.put(objValue)
                    }
                    if (objValue is JSONObject || objValue is JSONArray) {
                        scan(objValue)
                    }
                }
            }
            is JSONArray -> {
                val it = jsonValue.iterator()
                while (it.hasNext()) {
                    val value = it.next()
                    if (value is JSONObject || value is JSONArray) {
                        scan(value)
                    }
                }
            }
            else -> {}
        }
    }

    override fun read(json: Any): Any? {
        scan(json)
        return results
    }
}

internal interface Token {
    /**
     * Takes in JSONObject/JSONArray and outputs next JSONObject/JSONArray or value by evaluating token against current object/array in path
     * Unfortunately needs to be done with Any since [org.json.JSONObject] and [org.json.JSONArray] do not implement a common interface :(
     *
     * @param json [JSONObject] or [JSONArray]
     * @return [JSONObject], [JSONArray], or any JSON primitive value
     */
    fun read(json: Any): Any?
}