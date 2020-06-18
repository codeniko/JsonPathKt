package com.nfeld.jsonpathkt.util

import com.fasterxml.jackson.databind.node.ArrayNode
import com.nfeld.jsonpathkt.extension.children

internal class RootLevelArrayNode : ArrayNode {

    /**
     * Creates an empty RootLevelArrayNode
     */
    constructor() : super(JacksonUtil.mapper.nodeFactory)

    /**
     * Creates a RootLevelArrayNode with the children of the provided ArrayNode
     * */
    constructor(arrayNode: ArrayNode) : super(JacksonUtil.mapper.nodeFactory, arrayNode.children())
}
