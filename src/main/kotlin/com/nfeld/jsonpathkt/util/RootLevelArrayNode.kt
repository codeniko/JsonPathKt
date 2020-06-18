package com.nfeld.jsonpathkt.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.nfeld.jsonpathkt.extension.children

class RootLevelArrayNode : ArrayNode {

    /**
     * Creates an empty RootLevelArrayNode
     */
    constructor() : super(JacksonUtil.mapper.nodeFactory)


    /**
     * Creates a RootLevelArrayNode with the children of the provided ArrayNode
     * */
    constructor(arrayNode: ArrayNode) : super(JacksonUtil.mapper.nodeFactory, arrayNode.children())
    /**
     * Creates a RootLevelArrayNode with the provided children
     * */
    constructor(children: List<JsonNode>) : super(JacksonUtil.mapper.nodeFactory, children)

//    override fun equals(other: Any?): Boolean {
//        return other is RootLevelArrayNode && super.equals(other)
//    }

//    override fun equals(comparator: Comparator<JsonNode>?, o: JsonNode?): Boolean {
//        return o is RootLevelArrayNode && super.equals(comparator, o)
//    }

//    override fun hashCode(): Int {
//        // just want to differentiate it from other
//        val hashcode = super.hashCode()
//        return hashcode + (if (hashcode > 0) -1 else 1)
//    }
}