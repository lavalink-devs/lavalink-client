package dev.arbjerg.lavalink.client

import java.net.URI

class LavalinkClient(
    val userId: Long
) {
    private val internalNodes = mutableListOf<LavalinkNode>()

    // Non mutable public list
    val nodes: List<LavalinkNode> = internalNodes

    // TODO: allow to set session id?
    fun addNode(name: String, address: URI, password: String): LavalinkNode {
        val node = LavalinkNode(address, userId, password)
        internalNodes.add(node)

        return node
    }
}
