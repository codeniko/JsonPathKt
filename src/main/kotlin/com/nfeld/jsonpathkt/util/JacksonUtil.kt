package com.nfeld.jsonpathkt.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule

object JacksonUtil {
    val mapper: ObjectMapper by lazy {
        ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .registerModule(KotlinModule())
    }
}

internal inline fun createObjectNode() = JacksonUtil.mapper.createObjectNode()
internal inline fun createArrayNode() = JacksonUtil.mapper.createArrayNode()
