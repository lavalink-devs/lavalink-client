package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.loadbalancing.builtin.DefaultLoadBalancer
import dev.arbjerg.lavalink.client.loadbalancing.ILoadBalancer
import dev.arbjerg.lavalink.client.loadbalancing.IRegionFilter
import dev.arbjerg.lavalink.client.loadbalancing.VoiceRegion
import dev.arbjerg.lavalink.internal.ReconnectTask
import dev.arbjerg.lavalink.protocol.v4.Message
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.io.Closeable
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @param userId ID of the bot for authenticating with Discord
 */
class LavalinkClient(val userId: Long) : Closeable, Disposable {
    private val internalNodes = mutableListOf<LavalinkNode>()
    private val links = mutableMapOf<Long, Link>()

    // Immutable public list
    val nodes: List<LavalinkNode> = internalNodes

    // Events forwarded from all nodes.
    private val sink: Sinks.Many<ClientEvent<*>> = Sinks.many().multicast().onBackpressureBuffer()
    val flux: Flux<ClientEvent<*>> = sink.asFlux()
    private val reference: Disposable = flux.subscribe()

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
        reconnectService.scheduleWithFixedDelay(ReconnectTask(this), 0, 500, TimeUnit.MILLISECONDS)
    }

    // TODO: configure resuming

    /**
     * Add a node to the client.
     *
     * @param name The name of your node
     * @param address The ip and port of your node
     * @param password The password of your node
     * @param regionFilter (not currently used) Allows you to limit your node to a specific discord voice region
     */
    @JvmOverloads
    fun addNode(name: String, address: URI, password: String, regionFilter: IRegionFilter? = null): LavalinkNode {
        if (nodes.any { it.name == name }) {
            throw IllegalStateException("Node with name '$name' already exists")
        }

        val node = LavalinkNode(name, address, password, regionFilter, this)
        internalNodes.add(node)

        listenForNodeEvent(node)

        return node
    }

    /**
     * Get or crate a link between a guild and a node.
     *
     * @param guildId The id of the guild
     * @param region (not currently used) The target voice region of when to select a node
     */
    @JvmOverloads
    fun getLink(guildId: Long, region: VoiceRegion? = null): Link {
        if (guildId !in links) {
            val bestNode = loadBalancer.selectNode(region)
            links[guildId] = Link(guildId, bestNode)
        }

        return links[guildId]!!
    }

    /**
     * Returns a [Link] if it exists in teh cache.
     * If we select a region for voice updates we don't have a region to select.
     */
    internal fun getLinkIfCached(guildId: Long): Link? = links[guildId]

    internal fun onNodeDisconnected(node: LavalinkNode) {
        links.forEach { (_, link) ->
            if (link.node == node)  {
                link.transferNode(loadBalancer.selectNode(region = null))
            }
        }
    }

    // For the java people
    fun <T : ClientEvent<*>> on(type: Class<T>): Flux<T> {
        return flux.ofType(type)
    }

    inline fun <reified T : ClientEvent<*>> on() = on(T::class.java)

    /**
     * Close the client and disconnect all nodes.
     */
    override fun close() {
        reconnectService.shutdown()
        reference.dispose()
        nodes.forEach { it.close() }
    }

    override fun dispose() {
        close()
    }

    private fun listenForNodeEvent(node: LavalinkNode) {
        node.on<ClientEvent<Message>>()
            .subscribe {
                try {
                    sink.tryEmitNext(it)
                } catch (e: Exception) {
                    sink.tryEmitError(e)
                }
            }
    }
}
