package com.nfeld.jsonpathlite

import org.json.JSONArray
import org.json.JSONObject

// index can be negative
internal data class ArrayAccessorToken(val index: Int) : Token {
    override fun read(json: Any): Any? {
        if (json is JSONArray) {
            if (index < 0) {
                // optimized to get array length only if we're accessing from last
                val indexFromLast = json.length() + index
                if (indexFromLast >= 0) {
                    return json.opt(indexFromLast)
                }
            }
            return json.opt(index)
        }
        return null
    }
}

// indices can be negative
internal data class MultiArrayAccessorToken(val indices: List<Int>) : Token {
    private val result = JSONArray()

    override fun read(json: Any): Any? {
        if (json is JSONArray) {
            val jsonLength = json.length()
            indices.forEach { index ->
                if (index < 0) {
                    val indexFromLast = jsonLength + index
                    if (indexFromLast >= 0) {
                        json.opt(indexFromLast)?.let { result.put(it) }
                    }
                } else {
                    json.opt(index)?.let { result.put(it) }
                }
            }
            return result
        }
        return null
    }
}

internal data class ArrayToEndAccessorToken(val startIndex: Int, val offsetFromEnd: Int = 0) : Token {
    override fun read(json: Any): Any? {
        if (json is JSONArray) {
            val len = json.length()
            return MultiArrayAccessorToken(IntRange(startIndex, len - 1 + offsetFromEnd).toList()).read(json)
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

internal data class MultiObjectAccessorToken(val keys: List<String>) : Token {
    private val result = JSONObject()

    override fun read(json: Any): Any? {
        return if (json is JSONObject) {
            keys.forEach { key ->
                json.opt(key)?.let {
                    result.put(key, it)
                }
            }
            result
        } else null
    }
}

internal data class DeepScanToken(val targetKey: String) : Token {
    private val result = JSONArray()

    private fun scan(jsonValue: Any) {
        when (jsonValue) {
            is JSONObject -> {
                jsonValue.keySet().forEach { objKey ->
                    val objValue = jsonValue.opt(objKey)
                    if (objKey == targetKey) {
                        result.put(objValue)
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
        return result
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