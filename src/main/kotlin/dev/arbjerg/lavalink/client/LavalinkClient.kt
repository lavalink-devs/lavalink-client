package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.loadbalancing.DefaultLoadBalancer
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

    // Non mutable public list
    val nodes: List<LavalinkNode> = internalNodes
    var userId: Long = -1
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
    var loadBalancer: ILoadBalancer = DefaultLoadBalancer(this)

    private val reconnectService = Executors.newSingleThreadScheduledExecutor {
        val thread = Thread(it, "lavalink-reconnect-thread")
        thread.isDaemon = true
        thread
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

    @JvmOverloads
    fun addNode(name: String, address: URI, password: String, region: VoiceRegion = VoiceRegion.NONE): LavalinkNode {
        if (userId == -1L) {
            throw IllegalStateException("User ID not set, please use LavalinkClient#setUserId(Long) to set it before adding nodes.")
        }

        if (nodes.any { it.name == name }) {
            throw IllegalStateException("Node with name '$name' already exists")
        }

        val node = LavalinkNode(name, address, password, region, this)
        internalNodes.add(node)

        return node
    }

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

    fun onNodeDisconnected(node: LavalinkNode) {
        links.forEach { (_, link) ->
            if (link.node == node)  {
                link.node = loadBalancer.determineBestSocketForRegion(node.region)
            }
        }
    }

    fun onNodeConnected(node: LavalinkNode) {
        // TODO: do I need this?
    }

    override fun close() {
//        reconnectService.close()
        nodes.forEach { it.close() }
    }
}
