package dev.arbjerg.lavalink.client

import java.net.URI

class LavalinkClient {
    private val internalNodes = mutableListOf<Node>()

    // Non mutable public list
    val nodes: List<Node> = internalNodes

    fun addNode(name: String, address: URI, password: String) {
        TODO("Not yet implemented")
    }
}
