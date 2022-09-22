package com.nfeld.jsonpathkt.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object JacksonUtil {
    val mapper: ObjectMapper by lazy {
        jacksonObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
    }
}

internal inline fun createObjectNode() = JacksonUtil.mapper.createObjectNode()
internal inline fun createArrayNode() = JacksonUtil.mapper.createArrayNode()
