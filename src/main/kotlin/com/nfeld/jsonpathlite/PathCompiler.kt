package com.nfeld.jsonpathlite

object PathCompiler {

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
        val keyBuilder = StringBuilder()

        fun resetForNextToken() {
            isDeepScan = false
            keyBuilder.clear()
        }

        fun addObjectAccessorToken() {
            val key = keyBuilder.toString()
            if (isDeepScan) {
                tokens.add(DeepScanToken(key))
            } else {
                tokens.add(ObjectAccessorToken(key))
            }
        }

        val len = path.length
        var i = 1
        while (i < len) {
            val c = path[i]
            val next = path.getOrNull(i + 1)
            when {
                c == '.' -> {
                    if (keyBuilder.isNotEmpty()) {
                        addObjectAccessorToken()
                        resetForNextToken()
                    }
                    // check if it's followed by another dot. This means the following key will be used in deep scan
                    if (next == '.') {
                        isDeepScan = true
                        ++i
                    }
                }
                c == '[' -> {
                    if (keyBuilder.isNotEmpty()) {
                        addObjectAccessorToken()
                        resetForNextToken()
                    }
                    val closingBracketIndex = findMatchingClosingBracket(path, i)
                    if (closingBracketIndex > i + 1) { // i+1 checks to make sure atleast one char in the brackets
                        val token = compileBracket(path, i, closingBracketIndex)
                        tokens.add(token)
                        i = closingBracketIndex
                    } else {
                        throw IllegalArgumentException("Expecting closing array bracket with a value inside")
                    }
                }
                else -> keyBuilder.append(c)
            }
            ++i
        }

        if (keyBuilder.isNotEmpty()) {
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
                c == '\\' && expectingClosingQuote && next == '\'' -> {
                    ++i // skip this char so we don't process escaped quote
                }
            }
            ++i
        }

        return -1
    }

    /**
     * Compile path expression inside of brackets
     *
     * ['<name>' (, '<name>')]	Bracket-notated child or children
    [<number> (, <number>)]	Array index or indexes
    [start:end]
     [-<number>]
     [:end] all up to end, exclusive
     [start:] from <start> to end, inclusive
     [-<number>:] last number values
     *
     * @param path original path
     * @param openingIndex index of opening bracket
     * @param closingIndex index of closing bracket
     * @return Compiled [Token]
     */
    internal fun compileBracket(path: String, openingIndex: Int, closingIndex: Int): Token {
        var isObjectAccessor = false
        var isNegativeArrayAccessor = false // supplements isArrayAccessor
        var expectingClosingQuote = false
        var hasStartColon = false // found colon in beginning
        var hasEndColon = false // found colon in end
        var isRange = false // has starting and ending range. There will be two keys containing indices of each

        var i = openingIndex + 1
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

        //TODO handle escaped chars
        while (i < closingIndex) {
            val c = path[i]

            when {
                c == ' ' && !expectingClosingQuote -> {
                    // skip empty space that's not enclosed in quotes
                }

                c == ':' && !expectingClosingQuote -> {
                    if (openingIndex == i - 1) {
                        hasStartColon = true
                    } else if (i == closingIndex - 1) {
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

                c == ',' && !expectingClosingQuote -> {
                    // object accessor would have added key on closing quote
                    if (!isObjectAccessor && keyBuilder.isNotEmpty()) {
                        buildAndAddKey()
                    }
                }

                c == '\'' && expectingClosingQuote -> { // only valid inside array bracket and ending
                    if (keyBuilder.isEmpty()) {
                        throw IllegalArgumentException("Key is empty string")
                    }
                    buildAndAddKey()
                    expectingClosingQuote = false
                }

                c == '\'' -> {
                    expectingClosingQuote = true
                    isObjectAccessor = true
                }

                c.isDigit() || isObjectAccessor -> keyBuilder.append(c)
                else -> throw IllegalArgumentException("Unexpected char, char=$c, index=$i")
            }

            ++i
        }

        if (expectingClosingQuote) {
            throw IllegalArgumentException("Expecting closing quote in path")
        }

        if (keyBuilder.isNotEmpty()) {
            buildAndAddKey()
        }

        var token: Token? = null
        if (isObjectAccessor) {
            if (keys.size > 1) {
                token = MultiObjectAccessorToken(keys)
            } else {
                keys.firstOrNull()?.let {
                    token = ObjectAccessorToken(it)
                }
            }
        } else {
            when {
                isRange -> {
                    val start = keys[0].toInt(10)
                    val end = keys[1].toInt(10) // exclusive
                    val isEndNegative = end < 0
                    token = if (start < 0 || isEndNegative) {
                        val offsetFromEnd = if (isEndNegative) end else 0
                        val endIndex = if (!isEndNegative) end else null
                        ArrayLengthBasedRangeAccessorToken(start, endIndex, offsetFromEnd)
                    } else {
                        MultiArrayAccessorToken(IntRange(start, end - 1).toList())
                    }
                }
                hasStartColon -> {
                    val end = keys[0].toInt(10) // exclusive
                    token = if (end < 0) {
                        ArrayLengthBasedRangeAccessorToken(0, null, end)
                    } else {
                        MultiArrayAccessorToken(IntRange(0, end - 1).toList())
                    }
                }
                hasEndColon -> {
                    val start = keys[0].toInt(10)
                    token = ArrayLengthBasedRangeAccessorToken(start)
                }
                keys.size == 1 -> token = ArrayAccessorToken(keys[0].toInt(10))
                keys.size > 1 -> token = MultiArrayAccessorToken(keys.map { it.toInt(10) })
            }
        }

        token?.let {
            return it
        }

        throw IllegalArgumentException("Not a valid path")
    }
}