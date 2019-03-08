package com.nfeld.jsonpathlite

object PathCompiler {

    /**
     * @param path Path string to compile
     * @return List of [Token] to read against a JSON
     */
    @Throws(IllegalArgumentException::class)
    internal fun compile(path: String): List<Token> {

        val tokens = mutableListOf<Token>()

        if (path.firstOrNull() != '$') {
            throw IllegalArgumentException("First character in path must be '$' root token")
        }

        var isObjectAccessor = false
        var isArrayAccessor = false
        var isNegativeArrayAccessor = false // supplements isArrayAccessor
        var expectingClosingQuote = false
        var isDeepScan = false // supplements isObjectAccessor
        val keyBuilder = StringBuilder()

        fun resetForNextToken() {
            isObjectAccessor = false
            isArrayAccessor = false
            isNegativeArrayAccessor = false
            expectingClosingQuote = false
            isDeepScan = false
            keyBuilder.clear()
        }

        fun addObjectAccessorToken() {
            if (keyBuilder.isEmpty()) {
                throw IllegalArgumentException("Object key is empty in path")
            }
            val key = keyBuilder.toString()
            if (isDeepScan) {
                tokens.add(DeepScanToken(key))
            } else {
                tokens.add(ObjectAccessorToken(key))
            }
        }
        fun addArrayAccessorToken() {
            if (keyBuilder.isEmpty()) {
                throw IllegalArgumentException("Index of array is empty in path")
            }
            tokens.add(ArrayAccessorToken(keyBuilder.toString().toInt(10), isNegativeArrayAccessor))
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
                val next = path.getOrNull(i + 1)
                when {
                    c == '.' && !expectingClosingQuote -> {
                        if (isObjectAccessor || isArrayAccessor) {
                            if (keyBuilder.isEmpty()) {
                                // accessor symbol immediately after another access symbol, error
                                throw IllegalArgumentException("Unexpected char, char=$c, index=$i")
                            } else {
                                addAccessorToken()
                                resetForNextToken()
                            }
                        }
                        // check if it's followed by another dot. This means the following key will be used in deep scan
                        if (next == '.') {
                            isDeepScan = true
                            ++i
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
                                resetForNextToken()
                            }
                        }
                        when (next) {
                            '\'' -> {
                                ++i // skip already checked single quote
                                isObjectAccessor = true
                                expectingClosingQuote = true
                            }
                            '-' -> {
                                ++i
                                isArrayAccessor = true
                                isNegativeArrayAccessor = true
                            }
                            else -> isArrayAccessor = true
                        }
                    }
                    c == '\'' && expectingClosingQuote -> { // only valid inside array bracket and ending
                        if (next != ']') {
                            throw IllegalArgumentException("Expecting closing array bracket in path, index=${i+1}")
                        }
                        if (keyBuilder.length == 0) {
                            throw IllegalArgumentException("Key is empty string")
                        }
                        ++i // skip closing bracket
                        addObjectAccessorToken()
                        resetForNextToken()
                    }
                    c == ']' && !expectingClosingQuote -> {
                        if (!isArrayAccessor) {
                            throw IllegalArgumentException("Unexpected char, char=$c, index=$i")
                        }
                        if (expectingClosingQuote) {
                            throw IllegalArgumentException("Expecting closing single quote before closing bracket in path")
                        }
                        addArrayAccessorToken()
                        resetForNextToken()
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
}