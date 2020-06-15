package com.nfeld.jsonpathlite

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.nfeld.jsonpathlite.util.createArrayNode
import com.nfeld.jsonpathlite.util.createObjectNode

/**
 * Accesses value at [index] from [ArrayNode]
 *
 * @param index index to access, can be negative which means to access from end
 */
internal data class ArrayAccessorToken(val index: Int) : Token {
    override fun read(json: Any): Any? {
        if (json is ArrayNode) {
            if (index < 0) {
                // optimized to get array length only if we're accessing from last
                val indexFromLast = json.size() + index
                if (indexFromLast >= 0) {
                    return json.get(indexFromLast)
                }
            }
            return json.get(index)
        }
        return null
    }
}

/**
 * Accesses values at [indices] from [ArrayNode]. When read, value returned will be [ArrayNode] of values
 * at requested indices in given order.
 *
 * @param indices indices to access, can be negative which means to access from end
 */
internal data class MultiArrayAccessorToken(val indices: List<Int>) : Token {
    override fun read(json: Any): Any? {
        val result = createArrayNode()

        if (json is ArrayNode) {
            val size = json.size()
            indices.forEach { index ->
                if (index < 0) {
                    val indexFromLast = size + index
                    if (indexFromLast >= 0) {
                        json.get(indexFromLast)?.let { result.add(it) }
                    }
                } else {
                    json.get(index)?.let { result.add(it) }
                }
            }
        }
        return result
    }
}

/**
 * Accesses values from [ArrayNode] in range from [startIndex] to either [endIndex] or [offsetFromEnd] from end.
 * When read, value returned will be ArrayNode of values at requested indices in order of values in range.
 *
 * @param startIndex starting index of range, inclusive. Can be negative.
 * @param endIndex ending index of range, exclusive. Null if using [offsetFromEnd]
 * @param offsetFromEnd offset of values from end of array. 0 if using [endIndex]
 */
internal data class ArrayLengthBasedRangeAccessorToken(val startIndex: Int,
                                                       val endIndex: Int? = null,
                                                       val offsetFromEnd: Int = 0) : Token {
    override fun read(json: Any): Any? {
        val token = if (json is ArrayNode) {
             toMultiArrayAccessorToken(json)
        } else null
        return token?.read(json) ?: createArrayNode()
    }

    fun toMultiArrayAccessorToken(json: ArrayNode): MultiArrayAccessorToken? {
        val size = json.size()
        val start = if (startIndex < 0) {
            val start = size + startIndex
            if (start < 0) 0 else start // even if we're out of bounds at start, always start from first item
        } else startIndex

        // use endIndex if we have it, otherwise calculate from json array length
        val endInclusive = if (endIndex != null) {
            endIndex - 1
        } else size + offsetFromEnd - 1

        if (start >= 0 && endInclusive >= start) {
            return MultiArrayAccessorToken(IntRange(start, endInclusive).toList())
        }
        return MultiArrayAccessorToken(emptyList())
    }
}

/**
 * Accesses value at [key] from [ObjectNode]
 *
 * @param index index to access, can be negative which means to access from end
 */
internal data class ObjectAccessorToken(val key: String) : Token {
    override fun read(json: Any): Any? {
        return when (json)  {
            is ObjectNode -> {
                // println("ObjectAccessorToken" + json.get(key))
                json.get(key)
            }
            is ArrayNode -> {
                val result = createArrayNode()
                json.forEach {
                    (it as? ObjectNode)?.get(key)?.let {
                        result.add(it)
                    }
                }
                result
            }
            else -> null
        }
    }
}

/**
 * Accesses values at [keys] from [ObjectNode]. When read, value returned will be [ObjectNode]
 * containing key/value pairs requested. Keys that are null or don't exist won't be added in Object
 *
 * @param keys keys to access for which key/values to return
 */
internal data class MultiObjectAccessorToken(val keys: List<String>) : Token {
    override fun read(json: Any): Any? {

        return when (json) {
            is ObjectNode -> {
                multiKeyRead(json)
            }
            is ArrayNode -> {
                val result = createArrayNode()
                json.forEach {
                    if (it is ObjectNode) {
                        multiKeyRead(it)?.let { result.add(it) }
                    }
                }
                result
            }
            else -> null
        }
    }

    private inline fun multiKeyRead(node: ObjectNode): ObjectNode? {
        val result = createObjectNode()
        keys.forEach { key ->
            node.get(key)?.let {
                result.replace(key, it)
            }
        }
        return if (!result.isEmpty) result else null
    }
}

/**
 * Recursive scan for values with keys in [targetKeys] list. Returns a [ArrayNode] containing values found.
 *
 * @param targetKeys keys to find values for
 */
internal data class DeepScanObjectAccessorToken(val targetKeys: List<String>) : Token {
    private fun scan(jsonValue: Any, result: ArrayNode) {
        when (jsonValue) {
            is ObjectNode -> {
                // first add all values from keys requested to our result
                if (targetKeys.size > 1) {
                    val resultToAdd = createObjectNode()
                    targetKeys.forEach { targetKey ->
                        jsonValue.get(targetKey)?.let { resultToAdd.replace(targetKey, it) }
                    }
                    if (!resultToAdd.isEmpty) {
                        result.add(resultToAdd)
                    }
                } else {
                    targetKeys.firstOrNull()?.let { key ->
                        jsonValue.get(key)?.let { result.add(it) }
                    }
                }

                // recursively scan all underlying objects/arrays
                jsonValue.fieldNames().forEach { objKey ->
                    val objValue = jsonValue.get(objKey)
                    if (objValue is ObjectNode || objValue is ArrayNode) {
                        scan(objValue, result)
                    }
                }
            }
            is ArrayNode -> {
                jsonValue.forEach {
                    if (it is ObjectNode || it is ArrayNode) {
                        scan(it, result)
                    }
                }
            }
            else -> {}
        }
    }

    override fun read(json: Any): Any? {
        val result = createArrayNode()
        scan(json, result)
        return result
    }
}

/**
 * Recursive scan for values/objects/arrays found for all [indices] specified. Returns a [ArrayNode] containing results found.
 *
 * @param indices indices to retrieve values/objects for
 */
internal data class DeepScanArrayAccessorToken(val indices: List<Int>) : Token {
    private fun scan(jsonValue: Any, result: ArrayNode) {
        when (jsonValue) {
            is ObjectNode -> {
                // traverse all key/value pairs and recursively scan underlying objects/arrays
                jsonValue.fieldNames().forEach { objKey ->
                    val objValue = jsonValue.get(objKey)
                    if (objValue is ObjectNode || objValue is ArrayNode) {
                        scan(objValue, result)
                    }
                }
            }
            is ArrayNode -> {
                // first add all requested indices to our results
                indices.forEach { index ->
                    ArrayAccessorToken(index).read(jsonValue)?.let { result.add(it as JsonNode) }
                }

                // now recursively scan underlying objects/arrays
                jsonValue.forEach {
                    if (it is ObjectNode || it is ArrayNode) {
                        scan(it, result)
                    }
                }
            }
            else -> {}
        }
    }

    override fun read(json: Any): Any? {
        val result = createArrayNode()
        scan(json, result)
        return result
    }
}


/**
 * Recursive scan for values/objects/arrays from [ArrayNode] in range from [startIndex] to either [endIndex] or [offsetFromEnd] from end.
 * When read, value returned will be ArrayNode of values at requested indices in order of values in range. Returns a [ArrayNode] containing results found.
 *
 * @param startIndex starting index of range, inclusive. Can be negative.
 * @param endIndex ending index of range, exclusive. Null if using [offsetFromEnd]
 * @param offsetFromEnd offset of values from end of array. 0 if using [endIndex]
 */
internal data class DeepScanLengthBasedArrayAccessorToken(val startIndex: Int,
                                                          val endIndex: Int? = null,
                                                          val offsetFromEnd: Int = 0) : Token {
    private fun scan(jsonValue: Any, result: ArrayNode) {
        when (jsonValue) {
            is ObjectNode -> {
                // traverse all key/value pairs and recursively scan underlying objects/arrays
                jsonValue.fieldNames().forEach { objKey ->
                    val objValue = jsonValue.get(objKey)
                    if (objValue is ObjectNode || objValue is ArrayNode) {
                        scan(objValue, result)
                    }
                }
            }
            is ArrayNode -> {
                ArrayLengthBasedRangeAccessorToken(startIndex, endIndex, offsetFromEnd)
                    .toMultiArrayAccessorToken(jsonValue)
                    ?.read(jsonValue)
                    ?.let { resultAny ->
                        val resultArray = resultAny as? ArrayNode
                        resultArray?.forEach { result.add(it) }
                    }

                // now recursively scan underlying objects/arrays
                jsonValue.forEach {
                    if (it is ObjectNode || it is ArrayNode) {
                        scan(it, result)
                    }
                }
            }
            else -> {}
        }
    }

    override fun read(json: Any): Any? {
        val result = createArrayNode()
        scan(json, result)
        return result
    }
}

/**
 * Returns all values from an Object, or the same list
 */
internal class WildcardToken : Token {
    override fun read(json: Any): Any? {
        return when (json) {
            is ObjectNode -> {
                val result = createArrayNode()
                json.forEach { result.add(it) }
                result
            }
            else -> json
        }
    }

    override fun toString(): String = "WildcardToken"
    override fun hashCode(): Int = toString().hashCode()
    override fun equals(other: Any?): Boolean = other is WildcardToken
}

interface Token {
    /**
     * Takes in ObjectNode/ArrayNode and outputs next ObjectNode/ArrayNode or value by evaluating token against current object/array in path
     * Unfortunately needs to be done with Any since [org.json.ObjectNode] and [org.json.ArrayNode] do not implement a common interface :(
     *
     * @param json [ObjectNode] or [ArrayNode]
     * @return [ObjectNode], [ArrayNode], or any JSON primitive value
     */
    fun read(json: Any): Any?
}