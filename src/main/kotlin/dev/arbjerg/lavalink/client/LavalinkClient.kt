package dev.arbjerg.lavalink.client

import java.net.URI

class LavalinkClient {
    private val internalNodes = mutableListOf<LavalinkNode>()

    // Non mutable public list
    val nodes: List<LavalinkNode> = internalNodes

    fun addNode(name: String, address: URI, password: String): LavalinkNode {
        TODO("Not yet implemented")
    }
}
