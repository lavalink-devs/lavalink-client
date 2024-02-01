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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @param userId ID of the bot for authenticating with Discord
 */
class LavalinkClient(val userId: Long) : Closeable, Disposable {
    private val internalNodes = CopyOnWriteArrayList<LavalinkNode>()
    private val linkMap = ConcurrentHashMap<Long, Link>()
    private var clientOpen = true

    // Immutable public list
    val nodes: List<LavalinkNode>
        get() = internalNodes.toList()

    val links: List<Link>
        get() = linkMap.values.toList()

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
     * Remove a node by its [name].
     */
    fun removeNode(name: String): Boolean {
        val node = nodes.firstOrNull { it.name == name }

        if (node == null) {
            throw IllegalStateException("Node with name '$name' does not exist")
        }

        return removeNode(node)
    }

    /**
     * Disconnect and remove a node the client.
     */
    fun removeNode(node: LavalinkNode): Boolean {
        if (node !in internalNodes) {
            return false
        }

        node.close()

        internalNodes.remove(node)

        return true
    }

    /**
     * Get or crate a link between a guild and a node.
     *
     * @param guildId The id of the guild
     * @param region (not currently used) The target voice region of when to select a node
     */
    @JvmOverloads
    fun getLink(guildId: Long, region: VoiceRegion? = null): Link {
        if (!linkMap.containsKey(guildId)) {
            val bestNode = loadBalancer.selectNode(region)
            linkMap[guildId] = Link(guildId, bestNode)
            bestNode.playerCache[guildId] = newPlayer(bestNode, guildId.toString())
        }

        return linkMap[guildId]!!
    }

    /**
     * Returns a [Link] if it exists in the cache.
     * If we select a link for voice updates, we don't know the region yet.
     */
    fun getLinkIfCached(guildId: Long): Link? = linkMap[guildId]

    internal fun onNodeDisconnected(node: LavalinkNode) {
        // Don't do anything if we are shutting down.
        if (!clientOpen) {
            return
        }

        if (nodes.size == 1) {
            linkMap.forEach { (_, link) ->
                link.state = LinkState.DISCONNECTED
            }
            return
        }

        linkMap.forEach { (_, link) ->
            if (link.node == node)  {
                link.transferNode(loadBalancer.selectNode(region = null))
            }
        }
    }

    // For the java people
    /**
     * Listen to events from all nodes. Please note that uncaught exceptions will cause the listener to stop emitting events.
     *
     * @param type the [ClientEvent] to listen for
     *
     * @return a [Flux] of [ClientEvent]s
     */
    fun <T : ClientEvent<*>> on(type: Class<T>): Flux<T> {
        return flux.ofType(type)
    }

    /**
     * Listen to events from all nodes. Please note that uncaught exceptions will cause the listener to stop emitting events.
     *
     * @return a [Flux] of [ClientEvent]s
     */
    inline fun <reified T : ClientEvent<*>> on() = on(T::class.java)

    /**
     * Close the client and disconnect all nodes.
     */
    override fun close() {
        clientOpen = false
        reconnectService.shutdownNow()
        nodes.forEach { it.close() }
        reference.dispose()
    }

    override fun dispose() {
        close()
    }

    internal fun removeDestroyedLink(guildId: Long) {
        linkMap.remove(guildId)
    }

    private fun listenForNodeEvent(node: LavalinkNode) {
        node.on<ClientEvent<Message>>()
            .subscribe {
                try {
                    sink.tryEmitNext(it)
                } catch (e: Exception) {
                    sink.emitError(e, Sinks.EmitFailureHandler.FAIL_FAST)
                }
            }
    }
}
