package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.loadbalancing.RegionFilter
import dev.arbjerg.lavalink.internal.LavalinkRestClient
import dev.arbjerg.lavalink.internal.LavalinkSocket
import dev.arbjerg.lavalink.internal.loadbalancing.Penalties
import dev.arbjerg.lavalink.internal.toLavalinkPlayer
import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.arbjerg.lavalink.protocol.v4.Message
import dev.arbjerg.lavalink.protocol.v4.Stats
import okhttp3.OkHttpClient
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.Many
import reactor.kotlin.core.publisher.toMono
import java.io.Closeable
import java.net.URI

class LavalinkNode(
    val name: String,
    serverUri: URI,
    val password: String,
    val regionFilter: RegionFilter?,
    val lavalink: LavalinkClient
) : Disposable, Closeable {
    // "safe" uri with all paths removed
    val baseUri = "${serverUri.scheme}://${serverUri.host}:${serverUri.port}/v4"

    var sessionId: String? = null
        internal set

    internal val httpClient = OkHttpClient()

    internal val sink: Many<Message.EmittedEvent> = Sinks.many().multicast().onBackpressureBuffer()
    val flux: Flux<Message.EmittedEvent> = sink.asFlux()
    private val reference: Disposable = flux.subscribe()

    val rest = LavalinkRestClient(this)
    val ws = LavalinkSocket(this)

    // Stuff for load balancing
    val penalties = Penalties(this)
    var stats: Stats? = null
        internal set
    var available: Boolean = false
        internal set

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
    fun <T : Message.EmittedEvent> on(type: Class<T>): Flux<T> {
        return flux.ofType(type)
    }

    inline fun <reified T : Message.EmittedEvent> on() = on(T::class.java)

    /**
     * Retrieves a list of all players from the lavalink server.
     */
    fun getPlayers(): Mono<List<LavalinkPlayer>> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        return rest.getPlayers()
            .map { it.players.map { pl -> pl.toLavalinkPlayer(rest) } }
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
     */
    fun getPlayer(guildId: Long): Mono<LavalinkPlayer> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        if (guildId in playerCache) {
            return playerCache[guildId].toMono()
        }

        return rest.getPlayer(guildId)
            .map { it.toLavalinkPlayer(rest) }
            .doOnNext {
                // Update the player internally upon retrieving it.
                playerCache[it.guildId] = it
            }
    }

    /**
     * Destroy a player.
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

    /**
     * Load an item for the player.
     *
     * @param identifier The identifier (E.G. youtube url) to load.
     */
    fun loadItem(identifier: String): Mono<LoadResult> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        return rest.loadItem(identifier)
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
