package com.nfeld.jsonpathkt

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.nfeld.jsonpathkt.cache.CacheProvider
import com.nfeld.jsonpathkt.util.JacksonUtil

class JsonPath(path: String) {

    private val path: String
    val tokens: List<Token>

    /**
     * Trim given path string and compile it on initialization
     */
    init {
        this.path = path.trim()

        val cache = CacheProvider.getCache()
        val cachedJsonPath = cache?.get(this.path)
        if (cachedJsonPath != null) {
            tokens = cachedJsonPath.tokens
        } else {
            tokens = PathCompiler.compile(this.path)
            cache?.put(this.path, this)
        }
    }

    /**
     * Read the value at path in given JSON string
     *
     * @return Given type if value in path exists, null otherwise
     */
    inline fun <reified T : Any> readFromJson(jsonString: String): T? {
        return parse(jsonString)?.let { readFromJson(it) }
    }

    /**
     * Read the value at path in given jackson JsonNode Object
     *
     * @return Given type if value in path exists, null otherwise
     */
    inline fun <reified T : Any> readFromJson(json: JsonNode): T? {
        if (json.isMissingNode || json.isNull) {
            return null
        }

        val lastValue = tokens.fold(initial = json) { valueAtPath: JsonNode?, nextToken: Token ->
            valueAtPath?.let { nextToken.read(it) }
        }

        return when {
            lastValue == null || lastValue.isNull || lastValue.isMissingNode -> null
            else -> {
                try {
                    JacksonUtil.mapper.convertValue<T>(lastValue)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    /**
     * Check if a ArrayNode contains only primitive values (in this case, non-ObjectNode/ArrayNode).
     */
//    private fun containsOnlyPrimitives(arrayNode: ArrayNode) : Boolean {
//        arrayNode.forEach {
//            if (it.isObject || it.isArray) {
//                return false // fail fast
//            }
//        }
//        return true
//    }

//    private fun isSpecialChar(c: Char): Boolean {
//        return c == '"' || c == '\\' || c == '/' || c == 'b' || c == 'f' || c == 'n' || c == 'r' || c == 't'
//    }

    companion object {
        /**
         * Parse JSON string and return successful [JsonNode] or null otherwise
         *
         * @param jsonString JSON string to parse
         * @return instance of parsed jackson [JsonNode] object, or null
         */
        @JvmStatic
        fun parse(jsonString: String?): JsonNode? {
            return jsonString?.let {
                try {
                    val parsed = JacksonUtil.mapper.readTree(jsonString)
                    if (!parsed.isMissingNode && !parsed.isNull) {
                        parsed
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
        }

    }
}