package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.loadbalancing.DefaultLoadBalancer
import dev.arbjerg.lavalink.client.loadbalancing.ILoadBalancer
import dev.arbjerg.lavalink.client.loadbalancing.VoiceRegion
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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

    init {
        // TODO: replace this with a better system, this is just to have something that works for now
        val executor = Executors.newSingleThreadScheduledExecutor {
            val thread = Thread(it)
            thread.isDaemon = true
            thread
        }

        executor.scheduleAtFixedRate(
            {
                internalNodes.forEach { node ->
                    node.penalties.clearStats()
                }
            }, 1, 1, TimeUnit.MINUTES
        )
    }

    // Non mutable public list
    val nodes: List<LavalinkNode> = internalNodes

    @JvmOverloads
    fun addNode(name: String, address: URI, password: String, region: VoiceRegion = VoiceRegion.NONE): LavalinkNode {
        val node = LavalinkNode(name, address, password, region, this)
        internalNodes.add(node)

        return node
    }

    fun getLink(guildId: Long): Link {
        if (nodes.isEmpty()) {
            throw IllegalStateException("No available nodes!")
        }

        if (guildId !in links) {
            val bestNode = loadBalancer.determineBestNode()

            links[guildId] = Link(guildId, bestNode)
        }

        return links[guildId]!!
    }

    internal fun replaceLink(guildId: Long, newLink: Link) {
        TODO("Not implemented yet (do we even need this?)")
    }
}
