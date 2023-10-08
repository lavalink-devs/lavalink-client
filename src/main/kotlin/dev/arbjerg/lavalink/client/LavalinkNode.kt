package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.loadbalancing.IRegionFilter
import dev.arbjerg.lavalink.internal.LavalinkRestClient
import dev.arbjerg.lavalink.internal.LavalinkSocket
import dev.arbjerg.lavalink.internal.loadbalancing.Penalties
import dev.arbjerg.lavalink.internal.toLavalinkPlayer
import dev.arbjerg.lavalink.protocol.v4.*
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.Many
import reactor.kotlin.core.publisher.toMono
import java.io.Closeable
import java.io.IOException
import java.net.URI
import java.util.function.Consumer
import java.util.function.Function

class LavalinkNode(
    val name: String,
    val serverUri: URI,
    val password: String,
    val regionFilter: IRegionFilter?,
    val lavalink: LavalinkClient
) : Disposable, Closeable {
    // "safe" uri with all paths removed
    val baseUri = "${serverUri.scheme}://${serverUri.host}:${serverUri.port}/v4"

    var sessionId: String? = null
        internal set

    internal val httpClient = OkHttpClient()

    internal val sink: Many<ClientEvent<*>> = Sinks.many().multicast().onBackpressureBuffer()
    val flux: Flux<ClientEvent<*>> = sink.asFlux()
    private val reference: Disposable = flux.subscribe()

    internal val rest = LavalinkRestClient(this)
    val ws = LavalinkSocket(this)

    // Stuff for load balancing
    val penalties = Penalties(this)
    var stats: Stats? = null
        internal set
    var available: Boolean = false
        internal set

    // TODO: cache player per link instead?
    /**
     * A local player cache, allows us to not call the rest client every time we need a player.
     */
    internal val playerCache = mutableMapOf<Long, LavalinkPlayer>()

    override fun dispose() {
        reference.dispose()
    }

    override fun close() {
        ws.close()
        dispose()
    }

    // For the java people
    fun <T : ClientEvent<*>> on(type: Class<T>): Flux<T> {
        return flux.ofType(type)
    }

    inline fun <reified T : ClientEvent<*>> on() = on(T::class.java)

    /**
     * Retrieves a list of all players from the lavalink node.
     *
     * @return A list of all players from the node.
     */
    fun getPlayers(): Mono<List<LavalinkPlayer>> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        return rest.getPlayers()
            .map { it.players.map { pl -> pl.toLavalinkPlayer(this) } }
            .doOnNext {
                it.forEach { player ->
                    playerCache[player.guildId] = player
                }
            }
    }

    /**
     * Gets the player from the guild id. If the player is not cached, it will be retrieved from the server.
     *
     * @param guildId The guild id of the player.
     *
     * @Returns The player from the guild
     */
    fun getPlayer(guildId: Long): Mono<LavalinkPlayer> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        if (guildId in playerCache) {
            return playerCache[guildId].toMono()
        }

        return rest.getPlayer(guildId)
            .map { it.toLavalinkPlayer(this) }
            .onErrorResume { createOrUpdatePlayer(guildId).asMono() }
            .doOnNext {
                // Update the player internally upon retrieving it.
                playerCache[it.guildId] = it
            }
    }

    fun updatePlayer(guildId: Long, updateConsumer: Consumer<PlayerUpdateBuilder>): Mono<LavalinkPlayer> {
        val update = createOrUpdatePlayer(guildId)

        updateConsumer.accept(update)

        return update.asMono()
    }

    /**
     * Creates or updates a player.
     *
     * @param guildId The guild id that you want to create or update the player for.
     *
     * @return The newly created or updated player.
     */
    fun createOrUpdatePlayer(guildId: Long) = PlayerUpdateBuilder(this, guildId)

    /**
     * Destroy a guild's player.
     *
     * @param guildId The guild id of the player to destroy.
     */
    fun destroyPlayer(guildId: Long): Mono<Unit> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        return rest.destroyPlayer(guildId)
            .doOnNext {
                playerCache.remove(guildId)
            }
    }

    internal fun removeCachedPlayer(guildId: Long) {
        playerCache.remove(guildId)
    }

    /**
     * Load an item for the player.
     *
     * @param identifier The identifier (E.G. youtube url) to load.
     *
     * @return The [LoadResult] of whatever you tried to load.
     */
    fun loadItem(identifier: String): Mono<LoadResult> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        return rest.loadItem(identifier)
    }

    /**
     * Uses the node to decode a base64 encoded track.
     *
     * @param encoded The base64 encoded track to decode.
     *
     * @return The decoded track.
     */
    fun decodeTrack(encoded: String): Mono<Track> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        return rest.decodeTrack(encoded)
    }

    /**
     * Uses the node to decode a list of base64 encoded tracks.
     *
     * @param encoded The base64 encoded tracks to decode.
     *
     * @return The decoded tracks.
     */
    fun decodeTracks(encoded: List<String>): Mono<Tracks> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        return rest.decodeTracks(encoded)
    }

    /**
     * Get information about the node.
     */
    fun getNodeInfo(): Mono<Info> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        return rest.getNodeInfo()
    }

    /**
     * Send a custom request to the lavalink server. Any host and port you set will be replaced with the node host automatically.
     * The scheme must match your node's scheme, however.
     *
     * @param builderFn The request builder function, defaults such as the Authorization header have already been applied
     *
     * @return The Http response from the node
     */
    fun customRequest(builderFn: Function<Request.Builder, Request.Builder>): Mono<Response> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        val call = rest.newRequest {
            val request = builderFn.apply(this).build()
            val urlBuilder = request.url.newBuilder()
            val newBuilder = request.newBuilder()

            val newUrl = urlBuilder.host(serverUri.host)
                .port(serverUri.port)
                .build()

            newBuilder.url(newUrl)
        }

        return Mono.create { sink ->
            sink.onCancel {
                call.cancel()
            }

            call.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: Call, e: IOException) {
                    sink.error(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { res ->
                        sink.success(res)
                    }
                }
            })
        }
    }

    internal fun getCachedPlayer(guildId: Long): LavalinkPlayer? {
        if (guildId in playerCache) {
            return playerCache[guildId]
        }

        return null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LavalinkNode

        if (name != other.name) return false
        if (password != other.password) return false
        if (regionFilter != other.regionFilter) return false
        if (baseUri != other.baseUri) return false
        if (sessionId != other.sessionId) return false
        return available == other.available
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + regionFilter.hashCode()
        result = 31 * result + baseUri.hashCode()
        result = 31 * result + sessionId.hashCode()
        result = 31 * result + available.hashCode()
        return result
    }
}
