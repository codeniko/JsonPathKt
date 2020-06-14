package com.nfeld.jsonpathlite

import java.lang.IllegalStateException

internal object PathCompiler {

    /**
     * @param path Path string to compile
     * @return List of [Token] to read against a JSON
     */
    @Throws(IllegalArgumentException::class)
    internal fun compile(path: String): List<Token> {
        if (path.firstOrNull() != '$') {
            throw IllegalArgumentException("First character in path must be '$' root token")
        }

        val tokens = mutableListOf<Token>()
        var isDeepScan = false
        var isWildcard = false
        val keyBuilder = StringBuilder()

        fun resetForNextToken() {
            isDeepScan = false
            isWildcard = false
            keyBuilder.clear()
        }

        fun addObjectAccessorToken() {
            val key = keyBuilder.toString()
            when {
                isDeepScan -> tokens.add(DeepScanObjectAccessorToken(listOf(key)))
                isWildcard -> tokens.add(WildcardToken())
                else -> tokens.add(ObjectAccessorToken(key))
            }
        }

        val len = path.length
        var i = 1
        while (i < len) {
            val c = path[i]
            val next = path.getOrNull(i + 1)
            when {
                c == '.' -> {
                    if (keyBuilder.isNotEmpty() || isWildcard) {
                        addObjectAccessorToken()
                        resetForNextToken()
                    }
                    // check if it's followed by another dot. This means the following key will be used in deep scan
                    when (next) {
                        '.' -> {
                            isDeepScan = true
                            ++i
                        }
                        '*' -> {
                            isWildcard = true
                            ++i
                        }
                        null -> throw IllegalArgumentException("Unexpected ending with dot")
                    }
                }
                c == '[' -> {
                    if (keyBuilder.isNotEmpty() || isWildcard) {
                        addObjectAccessorToken()
                        resetForNextToken()
                    }
                    val closingBracketIndex = findMatchingClosingBracket(path, i)
                    if (closingBracketIndex > i + 1) { // i+1 checks to make sure atleast one char in the brackets
                        val token = compileBracket(path, i, closingBracketIndex)
                        if (isDeepScan) {
                            val deepScanToken: Token? = when (token) {
                                is ObjectAccessorToken -> DeepScanObjectAccessorToken(listOf(token.key))
                                is MultiObjectAccessorToken -> DeepScanObjectAccessorToken(token.keys)
                                is ArrayAccessorToken -> DeepScanArrayAccessorToken(listOf(token.index))
                                is MultiArrayAccessorToken -> DeepScanArrayAccessorToken(token.indices)
                                is ArrayLengthBasedRangeAccessorToken -> DeepScanLengthBasedArrayAccessorToken(token.startIndex, token.endIndex, token.offsetFromEnd)
                                else -> null
                            }
                            deepScanToken?.let { tokens.add(it) }
                            resetForNextToken()
                        } else {
                            tokens.add(token)
                        }
                        i = closingBracketIndex
                    } else {
                        throw IllegalArgumentException("Expecting closing array bracket with a value inside")
                    }
                }
                else -> keyBuilder.append(c)
            }
            ++i
        }

        if (keyBuilder.isNotEmpty() || isWildcard) {
            addObjectAccessorToken()
        }

        return tokens.toList()
    }

    /**
     * @param path original path
     * @param openingIndex opening bracket index we are to search matching closing bracket for
     * @return closing bracket index, or -1 if not found
     */
    internal fun findMatchingClosingBracket(path: String, openingIndex: Int): Int {
        var expectingClosingQuote = false
        var i = openingIndex + 1
        val len = path.length

        while (i < len) {
            val c = path[i]
            val next = path.getOrNull(i + 1)
            when {
                c == '\'' -> expectingClosingQuote = !expectingClosingQuote
                c == ']' && !expectingClosingQuote -> return i
                c == '\\' && expectingClosingQuote -> {
                    if (next == '\'' || next == '\\') {
                        ++i // skip this char so we don't process escaped quote
                    } else if (next == null) {
                        throw IllegalArgumentException("Unexpected char at end of path")
                    }
                }
            }
            ++i
        }

        return -1
    }

    /**
     * Compile path expression inside of brackets
     *
     * @param path original path
     * @param openingIndex index of opening bracket
     * @param closingIndex index of closing bracket
     * @return Compiled [Token]
     */
    internal fun compileBracket(path: String, openingIndex: Int, closingIndex: Int): Token {
        // isObjectAccessor is separate from expectingClosingQuote because the second you open a quote, it's always an object,
        // but we we can have multiple keys and thus multiple quotes opened for that object.
        var isObjectAccessor = false // once this is set, it cant be anything else
        var isNegativeArrayAccessor = false // supplements isArrayAccessor
        var isQuoteOpened = false // means we found an opening quote, so we expect a closing one to be valid
        var hasStartColon = false // found colon in beginning
        var hasEndColon = false // found colon in end
        var isRange = false // has starting and ending range. There will be two keys containing indices of each
        var isWildcard = false

        var i = openingIndex + 1
        var lastChar: Char = path[openingIndex]
        val keys = mutableListOf<String>()
        val keyBuilder = StringBuilder()

        fun buildAndAddKey() {
            var key = keyBuilder.toString()
            if (!isObjectAccessor && isNegativeArrayAccessor) {
                key = "-$key"
                isNegativeArrayAccessor = false
            }
            keys.add(key)
            keyBuilder.clear()
        }
        fun getNextCharIgnoringWhitespace(): Char {
            for (n in (i+1)..closingIndex) {
                val c = path[n]
                if (c == ' ' && !isQuoteOpened) {
                    continue
                }
                return c
            }
            throw IllegalStateException("")
        }
        fun isBracketNext() = getNextCharIgnoringWhitespace() == ']'
        fun isBracketBefore() = lastChar == '['

        //TODO handle escaped chars
        while (i < closingIndex) {
            val c = path[i]
            var setLastChar = true

            when {
                c == ' ' && !isQuoteOpened -> {
                    // skip empty space that's not enclosed in quotes
                    setLastChar = false
                }

                c == ':' && !isQuoteOpened -> {
                    if (isBracketBefore() && isBracketNext()) {
                        hasStartColon = true
                        hasEndColon = true
                    } else if (isBracketBefore()) {
                        hasStartColon = true
                    } else if (isBracketNext()) {
                        hasEndColon = true
                        // keybuilder should have a key...
                        buildAndAddKey()
                    } else if (keyBuilder.isNotEmpty()) {
                        buildAndAddKey() // becomes starting index of range
                        isRange = true
                    }
                }

                c == '-' && !isObjectAccessor -> {
                    isNegativeArrayAccessor = true
                }

                c == ',' && !isQuoteOpened -> {
                    // object accessor would have added key on closing quote
                    if (!isObjectAccessor && keyBuilder.isNotEmpty()) {
                        buildAndAddKey()
                    }
                }

                c == '\\' && isQuoteOpened -> {
                    val nextChar = path[i+1]
                    when (nextChar) {
                        '\\', '\'' -> {
                            keyBuilder.append(nextChar)
                            ++i
                        }
                    }
                }

                c == '\'' && isQuoteOpened -> { // only valid inside array bracket and ending
                    if (keyBuilder.isEmpty()) {
                        throw IllegalArgumentException("Key is empty string")
                    }
                    buildAndAddKey()
                    isQuoteOpened = false
                }

                c == '\'' && !isNegativeArrayAccessor -> {
                    isQuoteOpened = true
                    isObjectAccessor = true
                }

                c == '*' && !isQuoteOpened && isBracketBefore() && isBracketNext() -> {
                    isWildcard = true
                }

                c.isDigit() || isObjectAccessor -> keyBuilder.append(c)
                else -> throw IllegalArgumentException("Unexpected char, char=$c, index=$i")
            }

            ++i
            if (setLastChar) {
                lastChar = c
            }
        }

        if (keyBuilder.isNotEmpty()) {
            buildAndAddKey()
        }

        val token: Token? = if (isObjectAccessor) {
            if (keys.size > 1) {
                MultiObjectAccessorToken(keys)
            } else {
                keys.firstOrNull()?.let {
                    ObjectAccessorToken(it)
                }
            }
        } else {
            when {
                isWildcard -> WildcardToken()
                isRange -> {
                    val start = keys[0].toInt(10)
                    val end = keys[1].toInt(10) // exclusive
                    val isEndNegative = end < 0
                    if (start < 0 || isEndNegative) {
                        val offsetFromEnd = if (isEndNegative) end else 0
                        val endIndex = if (!isEndNegative) end else null
                        ArrayLengthBasedRangeAccessorToken(start, endIndex, offsetFromEnd)
                    } else {
                        MultiArrayAccessorToken(IntRange(start, end - 1).toList())
                    }
                }
                hasStartColon && hasEndColon -> {
                    // take entire list from beginning to end
                    ArrayLengthBasedRangeAccessorToken(0, null, 0)
                }
                hasStartColon -> {
                    val end = keys[0].toInt(10) // exclusive
                    if (end < 0) {
                        // take all from beginning to last minus $end
                        ArrayLengthBasedRangeAccessorToken(0, null, end)
                    } else {
                        // take all from beginning of list up to $end
                        MultiArrayAccessorToken(IntRange(0, end - 1).toList())
                    }
                }
                hasEndColon -> {
                    val start = keys[0].toInt(10)
                    ArrayLengthBasedRangeAccessorToken(start)
                }
                keys.size == 1 -> ArrayAccessorToken(keys[0].toInt(10))
                keys.size > 1 -> MultiArrayAccessorToken(keys.map { it.toInt(10) })
                else -> null
            }
        }

        token?.let {
            return it
        }

        throw IllegalArgumentException("Not a valid path")
    }
}