package com.nfeld.jsonpathlite

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class JsonPath(path: String) {

    private val path: String
    private val tokens: List<Token>

    /**
     * Trim given path string and compile it on initialization
     */
    init {
        this.path = path.trim()
        tokens = PathCompiler.compile(this.path)
    }

    /**
     * Read the value at path in given JSON string
     *
     * @return Given type if value in path exists, null otherwise
     */
    fun <T : Any> readFromJson(jsonString: String): T? {
        /*
        We don't need to parse this string into own JsonResult wrapper as we don't need those convenience methods at this point.
        Use org.json directly based on first character of given string. Also pass it to private readFromJson method directly to skip a stack frame
         */
        val trimmedJson = jsonString.trim()
        return when (trimmedJson.firstOrNull()) {
            '{' -> _readFromJson(JSONObject(trimmedJson))
            '[' -> _readFromJson(JSONArray(trimmedJson))
            else -> null
        }
    }

    /**
     * Read the value at path in given JSON Object
     *
     * @return Given type if value in path exists, null otherwise
     */
    fun <T : Any> readFromJson(jsonObject: JSONObject): T? = _readFromJson(jsonObject)

    /**
     * Read the value at path in given JSON Array
     *
     * @return Given type if value in path exists, null otherwise
     */
    fun <T : Any> readFromJson(jsonArray: JSONArray): T? = _readFromJson(jsonArray)

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> _readFromJson(json: Any): T? {
        var valueAtPath: Any? = json
        tokens.forEach { token ->
            valueAtPath?.let { valueAtPath = token.read(it) }
        }
        val lastValue = valueAtPath
        if (lastValue is JSONArray && containsOnlyPrimitives(lastValue)) {
            valueAtPath = lastValue.toList().toList() // return immutable list
        } else if (lastValue == JSONObject.NULL) {
            return null
        }
        return valueAtPath as? T
    }

    /**
     * Check if a JSONArray contains only primitive values (in this case, non-JSONObject/JSONArray).
     */
    private fun containsOnlyPrimitives(jsonArray: JSONArray) : Boolean {
        val it = jsonArray.iterator()
        while (it.hasNext()) {
            val item = it.next()
            if (item is JSONObject || item is JSONArray) {
                return false
            }
        }
        return true
    }

//    private fun isSpecialChar(c: Char): Boolean {
//        return c == '"' || c == '\\' || c == '/' || c == 'b' || c == 'f' || c == 'n' || c == 'r' || c == 't'
//    }

    companion object {
        /**
         * Parse JSON string and return successful [JsonResult] or throw [JSONException] on parsing error
         *
         * @param jsonString JSON string to parse
         * @return instance of parsed [JsonResult] object
         * @throws JSONException
         */
        @Throws(JSONException::class)
        @JvmStatic
        fun parse(jsonString: String): JsonResult = when {
            jsonString.isEmpty() -> throw JSONException("JSON string is empty")
            jsonString.first() == '{' -> JsonObject(JSONObject(jsonString))
            else -> JsonArray(JSONArray(jsonString))
        }

        /**
         * Parse JSON string and return successful [JsonResult] or null otherwise
         *
         * @param jsonString JSON string to parse
         * @return instance of parsed [JsonResult] object or null
         */
        @JvmStatic
        fun parseOrNull(jsonString: String): JsonResult? {
            return jsonString.firstOrNull()?.run {
                try {
                    if (this == '{') {
                        JsonObject(JSONObject(jsonString))
                    } else {
                        JsonArray(JSONArray(jsonString))
                    }
                } catch (e: JSONException) {
                    null
                }

            }
        }
    }
}