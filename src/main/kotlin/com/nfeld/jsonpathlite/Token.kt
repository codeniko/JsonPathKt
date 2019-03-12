package com.nfeld.jsonpathlite

import org.json.JSONArray
import org.json.JSONObject

/**
 * Accesses value at [index] from [JSONArray]
 *
 * @param index index to access, can be negative which means to access from end
 */
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

/**
 * Accesses values at [indices] from [JSONArray]. When read, value returned will be [JSONArray] of values
 * at requested indices in given order.
 *
 * @param indices indices to access, can be negative which means to access from end
 */
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

/**
 * Accesses values from [JSONArray] in range from [startIndex] to either [endIndex] or [offsetFromEnd] from end.
 * When read, value returned will be JSONArray of values at requested indices in order of values in range.
 *
 * @param startIndex starting index of range, inclusive. Can be negative.
 * @param endIndex ending index of range, exclusive. Null if using [offsetFromEnd]
 * @param offsetFromEnd offset of values from end of array. 0 if using [endIndex]
 */
internal data class ArrayLengthBasedRangeAccessorToken(val startIndex: Int,
                                                       val endIndex: Int? = null,
                                                       val offsetFromEnd: Int = 0) : Token {
    override fun read(json: Any): Any? {
        val token = if (json is JSONArray) {
             toMultiArrayAccessorToken(json)
        } else null
        return token?.read(json)
    }

    fun toMultiArrayAccessorToken(json: JSONArray): MultiArrayAccessorToken? {
        val len = json.length()
        val start = if (startIndex < 0) {
            len + startIndex
        } else startIndex

        // use endIndex if we have it, otherwise calculate from json array length
        val endInclusive = if (endIndex != null) {
            endIndex - 1
        } else len + offsetFromEnd - 1

        if (start >= 0 && endInclusive >= start) {
            return MultiArrayAccessorToken(IntRange(start, endInclusive).toList())
        }
        return MultiArrayAccessorToken(emptyList())
    }
}

/**
 * Accesses value at [key] from [JSONObject]
 *
 * @param index index to access, can be negative which means to access from end
 */
internal data class ObjectAccessorToken(val key: String) : Token {
    override fun read(json: Any): Any? {
        return if (json is JSONObject) {
            json.opt(key)
        } else null
    }
}

/**
 * Accesses values at [keys] from [JSONObject]. When read, value returned will be [JSONObject]
 * containing key/value pairs requested. Keys that are null or don't exist won't be added in Object
 *
 * @param keys keys to access for which key/values to return
 */
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

/**
 * Recursive scan for values with keys in [targetKeys] list. Returns a [JSONArray] containing values found.
 *
 * @param targetKeys keys to find values for
 */
internal data class DeepScanObjectAccessorToken(val targetKeys: List<String>) : Token {
    private val result = JSONArray()

    private fun scan(jsonValue: Any) {
        when (jsonValue) {
            is JSONObject -> {
                // first add all values from keys requested to result
                if (targetKeys.size > 1) {
                    val resultToAdd = JSONObject()
                    targetKeys.forEach { targetKey ->
                        jsonValue.opt(targetKey)?.let { resultToAdd.put(targetKey, it) }
                    }
                    if (!resultToAdd.isEmpty) {
                        result.put(resultToAdd)
                    }
                } else {
                    targetKeys.firstOrNull()?.let { key ->
                        jsonValue.opt(key)?.let { result.put(it) }
                    }
                }

                // recursively scan all underlying objects/arrays
                jsonValue.keySet().forEach { objKey ->
                    val objValue = jsonValue.opt(objKey)
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

/**
 * Recursive scan for values/objects/arrays found for all [indices] specified. Returns a [JSONArray] containing results found.
 *
 * @param indices indices to retrieve values/objects for
 */
internal data class DeepScanArrayAccessorToken(val indices: List<Int>) : Token {
    private val result = JSONArray()

    private fun scan(jsonValue: Any) {
        when (jsonValue) {
            is JSONObject -> {
                // traverse all key/value pairs and recursively scan underlying objects/arrays
                jsonValue.keySet().forEach { objKey ->
                    val objValue = jsonValue.opt(objKey)
                    if (objValue is JSONObject || objValue is JSONArray) {
                        scan(objValue)
                    }
                }
            }
            is JSONArray -> {
                // first add all requested indices to our results
                indices.forEach { index ->
                    ArrayAccessorToken(index).read(jsonValue)?.let { result.put(it) }
                }

                // now recursively scan underlying objects/arrays
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