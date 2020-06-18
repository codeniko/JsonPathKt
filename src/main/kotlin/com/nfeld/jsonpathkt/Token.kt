package com.nfeld.jsonpathkt

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.nfeld.jsonpathkt.extension.getValueIfNotNullOrMissing
import com.nfeld.jsonpathkt.extension.isNotNullOrMissing
import com.nfeld.jsonpathkt.util.RootLevelArrayNode
import com.nfeld.jsonpathkt.util.createArrayNode

/**
 * Accesses value at [index] from [ArrayNode]
 *
 * @param index index to access, can be negative which means to access from end
 */
internal data class ArrayAccessorToken(val index: Int) : Token {
    override fun read(json: JsonNode): JsonNode? {
        return read(json, index)
    }

    companion object {
        fun read(json: JsonNode, index: Int): JsonNode? {
            return when (json) {
                is RootLevelArrayNode -> {
                    // iterate through all items on root and get items from all sublists
                    val result = RootLevelArrayNode()
                    json.forEach { node ->
                        read(node, index)?.let {
                            if (it.isNotNullOrMissing()) {
                                result.add(it)
                            }
                        }
                    }
                    result
                }
                is ArrayNode -> {
                    // get the value at index directly
                    readValueAtIndex(json, index)
                }
                is TextNode -> {
                    val str = json.asText()
                    if (index < 0) {
                        val indexFromLast = str.length + index
                        if (indexFromLast >= 0 && indexFromLast < str.length) {
                            return TextNode(str[indexFromLast].toString())
                        } else null
                    } else if (index >= 0 && index < str.length) {
                        TextNode(str[index].toString())
                    } else null
                }
                else -> null
            }
        }

        private fun readValueAtIndex(arrayNode: ArrayNode, index: Int): JsonNode? {
            if (index < 0) {
                val indexFromLast = arrayNode.size() + index
                if (indexFromLast >= 0) {
                    return arrayNode.getValueIfNotNullOrMissing(indexFromLast)
                }
            }
            return arrayNode.getValueIfNotNullOrMissing(index)
        }
    }
}

/**
 * Accesses values at [indices] from [ArrayNode]. When read, value returned will be [ArrayNode] of values
 * at requested indices in given order.
 *
 * @param indices indices to access, can be negative which means to access from end
 */
internal data class MultiArrayAccessorToken(val indices: List<Int>) : Token {
    override fun read(json: JsonNode): JsonNode? {
        val result = RootLevelArrayNode()
        when (json) {
            is RootLevelArrayNode -> {
                // needs to be flattened, thus we iterate for each subnode before passing the reading down
                json.forEach { node ->
                    indices.forEach {
                        ArrayAccessorToken.read(node, it)?.let {
                            if (it.isNotNullOrMissing()) {
                                result.add(it)
                            }
                        }
                    }
                }
            }
            else -> {
                indices.forEach {
                    ArrayAccessorToken.read(json, it)?.let {
                        if (it.isNotNullOrMissing()) {
                            result.add(it)
                        }
                    }
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
 * @param endIndex ending index of range, exclusive. Null if using [offsetFromEnd]. Can be positive only
 * @param offsetFromEnd offset of values from end of array. 0 if using [endIndex]. Can be negative only
 */
internal data class ArrayLengthBasedRangeAccessorToken(val startIndex: Int,
                                                       val endIndex: Int? = null,
                                                       val offsetFromEnd: Int = 0) : Token {
    override fun read(json: JsonNode): JsonNode? {
        val token = when (json) {
            is RootLevelArrayNode -> {
                val result = RootLevelArrayNode()
                json.forEach { node ->
                    read(node)?.let {
                        // needs to be flattened so we add each underlying result to our result
                        if (it is ArrayNode) {
                            it.forEach {
                                result.add(it)
                            }
                        } else if (it.isNotNullOrMissing()) {
                            result.add(it)
                        } else null
                    }
                }
                return result
            }
            is ArrayNode -> toMultiArrayAccessorToken(json)
            else -> null
        }
        return token?.read(json) ?: RootLevelArrayNode()
    }

    /**
     * We know the size of the array during runtime so we can recreate the MultiArrayAccessorToken to read the values
     */
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

        return if (start >= 0 && endInclusive >= start) {
            MultiArrayAccessorToken(IntRange(start, endInclusive).toList())
        } else null
    }
}

/**
 * Accesses value at [key] from [ObjectNode]
 *
 * @param index index to access, can be negative which means to access from end
 */
internal data class ObjectAccessorToken(val key: String) : Token {
    override fun read(json: JsonNode): JsonNode? {
        return read(json, key)
    }

    companion object {
        fun read(json: JsonNode, key: String): JsonNode? {
            return when (json)  {
                is ObjectNode -> json.getValueIfNotNullOrMissing(key)
                is RootLevelArrayNode -> {
                    // we're at root level and can get children from objects
                    val result = RootLevelArrayNode()
                    json.forEach {
                        (it as? ObjectNode)?.let { obj ->
                            obj.getValueIfNotNullOrMissing(key)?.let { result.add(it) }
                        }
                    }
                    result
                }
                // ArrayNode should return null, unless it's the RootLevelArrayNode. This is intentional
                // everything else is scalar and not accessible
                else -> null
            }
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
    override fun read(json: JsonNode): JsonNode? {

        return when (json) {
            is ObjectNode -> {
                // Going from an object to a list always creates a root level list
                val result = RootLevelArrayNode()
                keys.forEach {
                    json.getValueIfNotNullOrMissing(it)?.let { result.add(it) }
                }
                result
            }
            is RootLevelArrayNode -> {
                val result = RootLevelArrayNode()
                json.forEach { node ->
                    keys.forEach { key ->
                        ObjectAccessorToken.read(node, key)?.let {
                            if (it.isNotNullOrMissing()) {
                                result.add(it)
                            }
                        }
                    }
                }
                result
            }
            else -> RootLevelArrayNode()
        }
    }
}

/**
 * Recursive scan for values with keys in [targetKeys] list. Returns a [ArrayNode] containing values found.
 *
 * @param targetKeys keys to find values for
 */
internal data class DeepScanObjectAccessorToken(val targetKeys: List<String>) : Token {
    private fun scan(node: JsonNode, result: ArrayNode) {
        when (node) {
            is ObjectNode -> {
                // first add all values from keys requested to our result
                targetKeys.forEach { key ->
                    ObjectAccessorToken.read(node, key)?.let {
                        if (it.isNotNullOrMissing()) {
                            result.add(it)
                        }
                    }
                }

                // recursively scan all underlying objects/arrays
                node.forEach {
                    if (it.isNotNullOrMissing()) {
                        scan(it, result)
                    }
                }
            }
            is ArrayNode -> {
                node.forEach {
                    if (it.isNotNullOrMissing()) {
                        scan(it, result)
                    }
                }
            }
            else -> {}
        }
    }

    override fun read(json: JsonNode): JsonNode? {
        val result = RootLevelArrayNode()
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
    private fun scan(node: JsonNode, result: ArrayNode) {
        when (node) {
            is ObjectNode -> {
                // traverse all key/value pairs and recursively scan underlying objects/arrays
                node.forEach {
                    if (it.isNotNullOrMissing()) {
                        scan(it, result)
                    }
                }
            }
            is RootLevelArrayNode -> {
                // no need to add anything on root level, scan down next level
                node.forEach {
                    if (it.isNotNullOrMissing()) {
                        scan(it, result)
                    }
                }
            }
            is ArrayNode -> {
                // first add all requested indices to our results
                indices.forEach { index ->
                    ArrayAccessorToken(index).read(node)?.let {
                        if (it.isNotNullOrMissing()) {
                            result.add(it)
                        }
                    }
                }

                // now recursively scan underlying objects/arrays
                node.forEach {
                    if (it.isNotNullOrMissing()) {
                        scan(it, result)
                    }
                }
            }
            else -> {}
        }
    }

    override fun read(json: JsonNode): JsonNode? {
        val result = RootLevelArrayNode()
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
    private fun scan(node: JsonNode, result: ArrayNode) {
        when (node) {
            is ObjectNode -> {
                // traverse all key/value pairs and recursively scan underlying objects/arrays
                node.forEach {
                    if (it.isNotNullOrMissing()) {
                        scan(it, result)
                    }
                }
            }
            is RootLevelArrayNode -> {
                // no need to add anything on root level, scan down next level
                node.forEach {
                    if (it.isNotNullOrMissing()) {
                        scan(it, result)
                    }
                }
            }
            is ArrayNode -> {
                ArrayLengthBasedRangeAccessorToken(startIndex, endIndex, offsetFromEnd)
                    ?.read(node)
                    ?.let { resultAny ->
                        val resultArray = resultAny as? ArrayNode
                        resultArray?.forEach { result.add(it) }
                    }

                // now recursively scan underlying objects/arrays
                node.forEach {
                    if (it.isNotNullOrMissing()) {
                        scan(it, result)
                    }
                }
            }
            else -> {}
        }
    }

    override fun read(json: JsonNode): JsonNode? {
        val result = RootLevelArrayNode()
        scan(json, result)
        return result
    }
}

/**
 * Returns all values from an Object, or the same list
 */
internal class WildcardToken : Token {
    override fun read(json: JsonNode): JsonNode? {
        return when (json) {
            is ObjectNode -> {
                val result = RootLevelArrayNode()
                json.forEach {
                    if (it.isNotNullOrMissing()) {
                        result.add(it)
                    }
                }
                result
            }
            is ArrayNode -> {
                if (json !is RootLevelArrayNode) {
                    // copy over children into our special ArrayNode to hold underlying items
                    RootLevelArrayNode(json)
                } else {
                    val result = RootLevelArrayNode()
                    // iterate through each item and move everything up one level
                    json.forEach { node ->
                        when (node) {
                            is ObjectNode -> {
                                node.forEach {
                                    if (it.isNotNullOrMissing()) {
                                        result.add(it)
                                    }
                                }
                            }
                            is ArrayNode -> {
                                // move all items from this node to result node
                                node.forEach {
                                    if (it.isNotNullOrMissing()) {
                                        result.add(it)
                                    }
                                }
                            }
                            // anything else gets dropped since it's on rootmost level
                        }
                    }
                    result
                }
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
     * Takes in JsonNode and outputs next JsonNode or value by evaluating token against current object/array in path
     */
    fun read(json: JsonNode): JsonNode?
}