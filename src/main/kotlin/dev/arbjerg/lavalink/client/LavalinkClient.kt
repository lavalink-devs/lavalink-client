package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.internal.Link
import java.net.URI

class LavalinkClient(
    val userId: Long
) {
    private val internalNodes = mutableListOf<LavalinkNode>()
    private val links = mutableMapOf<Long, Link>()

    // Non mutable public list
    val nodes: List<LavalinkNode> = internalNodes

    fun addNode(name: String, address: URI, password: String): LavalinkNode {
        val node = LavalinkNode(address, userId, password)
        internalNodes.add(node)

        return node
    }

    // TODO: how to get best node for guild?
    fun getLink(guildId: Long): Link {
        if (guildId !in links) {
            links[guildId] = Link(guildId, internalNodes.random())
        }

        return links[guildId]!!
    }
}
