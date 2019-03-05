package com.nfeld.jsonpathlite

import org.jetbrains.annotations.TestOnly
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class JsonPath(path: String) {

    private data class ArrayAccessorToken(val index: Int) : Token {
        override fun read(json: Any): Any? {
            return if (json is JSONArray) {
                try {
                    json.get(index)
                } catch (e: JSONException) {
                    null
                }
            } else null
        }
    }
    private data class ObjectAccessorToken(val key: String) : Token {
        override fun read(json: Any): Any? {
            return if (json is JSONObject) {
                try {
                    json.get(key)
                } catch (e: JSONException) {
                    null
                }
            } else null
        }
    }
    private interface Token {
        fun read(json: Any): Any? // takes in JSONObject/JSONArray and outputs next JSONObject/JSONArray or value
    }

    private val path: String
    private val tokens: List<Token>

    init {
        this.path = path.trim()
        tokens = compile()

        if (tokens.isEmpty()) {
            // todo?
        }
    }

    /**
     * Read the value at path in given JSON
     *
     * @return Given type if value in path exists, null otherwise
     */
    fun <T : Any> readFromJson(jsonString: String): T? {
        // given string, we need to find if it's json object or array
        val trimmedJson = jsonString.trim()
        return when (trimmedJson.firstOrNull()) {
            '{' -> _readFromJson(JSONObject(trimmedJson))
            '[' -> _readFromJson(JSONArray(trimmedJson))
            else -> null
        }
    }

    /**
     * Read the value at path in given JSON
     *
     * @return Given type if value in path exists, null otherwise
     */
    fun <T : Any> readFromJson(jsonObject: JSONObject): T? = _readFromJson(jsonObject)

    /**
     * Read the value at path in given JSON
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
        return valueAtPath as? T
    }

    /**
     * Compile jsonpath
     */
    @Throws(IllegalArgumentException::class)
    private fun compile(): List<Token> {

        val tokens = mutableListOf<Token>()

        if (path.firstOrNull() != '$') {
            throw IllegalArgumentException("First character in path must be '$' root token")
        }

        var isObjectAccessor = false
        var isArrayAccessor = false
        var expectingClosingQuote = false
        val keyBuilder = StringBuilder()

        fun addObjectAccessorToken() {
            if (keyBuilder.isEmpty()) {
                throw IllegalArgumentException("Object key is empty in path")
            }
            tokens.add(ObjectAccessorToken(keyBuilder.toString()))
        }
        fun addArrayAccessorToken() {
            if (keyBuilder.isEmpty()) {
                throw IllegalArgumentException("Index of array is empty in path")
            }
            tokens.add(ArrayAccessorToken(keyBuilder.toString().toInt(10)))
        }
        fun addAccessorToken() {
            if (isObjectAccessor) {
                addObjectAccessorToken()
            } else {
                addArrayAccessorToken()
            }
        }

        val len = path.length
        var i = 1
        try {
            while (i < len) {
                val c = path[i]
                when {
                    c == '.' && !expectingClosingQuote -> {
                        if (isObjectAccessor || isArrayAccessor) {
                            if (keyBuilder.isEmpty()) {
                                // accessor symbol immediately after another access symbol, error
                                throw IllegalArgumentException("Unexpected char, char=$c, index=$i")
                            } else {
                                addAccessorToken()
                                keyBuilder.clear()
                                isArrayAccessor = false
                            }
                        }
                        isObjectAccessor = true
                    }
                    c == '[' && !expectingClosingQuote -> {
                        if (isObjectAccessor || isArrayAccessor) {
                            if (keyBuilder.isEmpty()) {
                                // accessor symbol immediately after another access symbol, error
                                throw IllegalArgumentException("Unexpected char, char=$c, index=$i")
                            } else {
                                addObjectAccessorToken()
                                keyBuilder.clear()
                                isObjectAccessor = false
                                isArrayAccessor = false
                            }
                        }
                        if (path[i + 1] == '\'') {
                            ++i // skip already checked single quote
                            isObjectAccessor = true
                            expectingClosingQuote = true
                        } else {
                            isArrayAccessor = true
                        }
                    }
                    c == '\'' && expectingClosingQuote -> { // only valid inside array bracket and ending
                        if (path[i + 1] != ']') {
                            throw IllegalArgumentException("Expecting closing array bracket in path, index=${i+1}")
                        }
                        if (keyBuilder.length == 0) {
                            throw IllegalArgumentException("Key is empty string")
                        }
                        ++i // skip closing bracket
                        expectingClosingQuote = false
                        isObjectAccessor = false
                        addObjectAccessorToken()
                        keyBuilder.clear()
                    }
                    c == ']' && !expectingClosingQuote -> {
                        if (!isArrayAccessor) {
                            throw IllegalArgumentException("Unexpected char, char=$c, index=$i")
                        }
                        if (expectingClosingQuote) {
                            throw IllegalArgumentException("Expecting closing single quote before closing bracket in path")
                        }
                        isArrayAccessor = false
                        addArrayAccessorToken()
                        keyBuilder.clear()
                    }
                    c.isDigit() && isArrayAccessor -> keyBuilder.append(c)
                    isObjectAccessor -> keyBuilder.append(c)
                    else -> throw IllegalArgumentException("Unexpected char, char=$c, index=$i")
                }
                ++i
            }

            // Object accessor is the only one able to `true` at this point
            if (expectingClosingQuote || isArrayAccessor) {
                throw IllegalArgumentException("Expecting closing array in path at end")
            }

            if (keyBuilder.isNotEmpty()) {
                if (isObjectAccessor) {
                    addObjectAccessorToken()
                } else {
                    throw IllegalArgumentException("Expecting closing array in path at end")
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalArgumentException("Path is invalid")
        }

        return tokens.toList()
    }

    @TestOnly
    fun printTokens() {
        println("Tokens: " + tokens.toString())
    }

    companion object {
        /**
         * Parse json string and return [JsonResult] or throw [JSONException] on parsing error
         */
        @Throws(JSONException::class)
        fun parse(jsonString: String): JsonResult = when {
            jsonString.isEmpty() -> throw JSONException("JSON string is empty")
            jsonString.first() == '{' -> JsonObject(JSONObject(jsonString))
            else -> JsonArray(JSONArray(jsonString))
        }

        /**
         * Parse JSON string and return [JsonResult] or null otherwise
         */
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