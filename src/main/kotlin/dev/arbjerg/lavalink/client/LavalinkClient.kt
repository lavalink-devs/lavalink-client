package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.loadbalancing.builtin.DefaultLoadBalancer
import dev.arbjerg.lavalink.client.loadbalancing.ILoadBalancer
import dev.arbjerg.lavalink.client.loadbalancing.VoiceRegion
import dev.arbjerg.lavalink.internal.ReconnectTask
import java.io.Closeable
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LavalinkClient: Closeable {
    private val internalNodes = mutableListOf<LavalinkNode>()
    private val links = mutableMapOf<Long, Link>()

    // Immutable public list
    val nodes: List<LavalinkNode> = internalNodes
    var userId: Long? = null
        set(value) {
            if (nodes.isNotEmpty()) {
                throw IllegalStateException("Can't set userId if we already have nodes registered!")
            }

            field = value
        }

    /**
     * To determine the best node, we use a load balancer.
     * It is recommended to not change the load balancer after you've connected to a voice channel.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var loadBalancer: ILoadBalancer = DefaultLoadBalancer(this)

    private val reconnectService = Executors.newSingleThreadScheduledExecutor {
        Thread(it, "lavalink-reconnect-thread").apply { isDaemon = true }
    }

    init {
        reconnectService.scheduleWithFixedDelay(ReconnectTask(this), 0, 500, TimeUnit.MILLISECONDS);

        // TODO: replace this with a better system, this is just to have something that works for now
        reconnectService.scheduleAtFixedRate(
            {
                internalNodes.forEach { node ->
                    node.penalties.clearStats()
                }
            }, 1, 1, TimeUnit.MINUTES
        )
    }

    // TODO: configure resuming

    /**
     * Add a node to the client.
     *
     * @param name The name of your node
     * @param address The ip and port of your node
     * @param password The password of your node
     * @param region (not currently used) The voice region of your node
     */
    @JvmOverloads
    fun addNode(name: String, address: URI, password: String, region: VoiceRegion = VoiceRegion.NONE): LavalinkNode {
        if (userId == null) {
            throw IllegalStateException("User ID not set, please use LavalinkClient#setUserId(Long) to set it before adding nodes.")
        }

        if (nodes.any { it.name == name }) {
            throw IllegalStateException("Node with name '$name' already exists")
        }

        val node = LavalinkNode(name, address, password, region, this)
        internalNodes.add(node)

        return node
    }

    /**
     * Get or crate a link between a guild and a node.
     *
     * @param guildId The id of the guild
     * @param region (not currently used) The target voice region of when to select a node
     */
    @JvmOverloads
    fun getLink(guildId: Long, region: VoiceRegion = VoiceRegion.NONE): Link {
        if (nodes.isEmpty()) {
            throw IllegalStateException("No available nodes!")
        }

        if (guildId !in links) {
            val bestNode = loadBalancer.determineBestSocketForRegion(region)

            links[guildId] = Link(guildId, bestNode)
        }

        return links[guildId]!!
    }

    internal fun onNodeDisconnected(node: LavalinkNode) {
        links.forEach { (_, link) ->
            if (link.node == node)  {
                link.transferNode(loadBalancer.determineBestSocketForRegion(node.region))
            }
        }
    }

    internal fun onNodeConnected(node: LavalinkNode) {
        // TODO: do I need this?
    }

    /**
     * Close the client and disconnect all nodes.
     */
    override fun close() {
//        reconnectService.close()
        nodes.forEach { it.close() }
    }
}
