package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.loadbalancing.DefaultLoadBalancer
import dev.arbjerg.lavalink.client.loadbalancing.ILoadBalancer
import dev.arbjerg.lavalink.client.loadbalancing.VoiceRegion
import java.net.URI

class LavalinkClient(
    val userId: Long
) {
    private val internalNodes = mutableListOf<LavalinkNode>()
    private val links = mutableMapOf<Long, Link>()

    /**
     * To determine the best node, we use a load balancer.
     * It is recommended to not change the load balancer after you've connected to a voice channel.
     */
    var loadBalancer: ILoadBalancer = DefaultLoadBalancer(this)

    // Non mutable public list
    val nodes: List<LavalinkNode> = internalNodes

    @JvmOverloads
    fun addNode(name: String, address: URI, password: String, region: VoiceRegion = VoiceRegion.NONE): LavalinkNode {
        val node = LavalinkNode(address, userId, password, region)
        internalNodes.add(node)

        return node
    }

    // TODO: how to get best node for guild?
    fun getLink(guildId: Long): Link {
        if (nodes.isEmpty()) {
            throw IllegalStateException("No nodes are connected.")
        }

        if (guildId !in links) {
            val bestNode = loadBalancer.determineBestNode() ?: throw IllegalStateException("Could not determine a node to connect to.")

            links[guildId] = Link(guildId, bestNode)
        }

        return links[guildId]!!
    }

    internal fun replaceLink(guildId: Long, newLink: Link) {
        TODO("Not implemented yet")
    }
}
