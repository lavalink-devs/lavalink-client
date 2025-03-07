package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.event.ClientEvent
import dev.arbjerg.lavalink.client.event.ResumeSynchronizationEvent
import dev.arbjerg.lavalink.client.http.HttpBuilder
import dev.arbjerg.lavalink.client.player.*
import dev.arbjerg.lavalink.client.player.Track
import dev.arbjerg.lavalink.client.player.toCustom
import dev.arbjerg.lavalink.internal.*
import dev.arbjerg.lavalink.internal.error.RestException
import dev.arbjerg.lavalink.internal.loadbalancing.Penalties
import dev.arbjerg.lavalink.internal.toLavalinkPlayer
import dev.arbjerg.lavalink.protocol.v4.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import org.slf4j.LoggerFactory
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.Many
import reactor.kotlin.core.publisher.toMono
import reactor.util.retry.Retry
import java.io.Closeable
import java.io.IOException
import java.time.Duration
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.UnaryOperator

/**
 * The Node is a physical instance of the lavalink server software.
 */
class LavalinkNode(
    private val nodeOptions: NodeOptions,
    val lavalink: LavalinkClient
) : Disposable, Closeable {
    private val logger = LoggerFactory.getLogger(LavalinkNode::class.java)
    // "safe" uri with all paths removed
    val baseUri = "${nodeOptions.serverUri.scheme}://${nodeOptions.serverUri.host}:${nodeOptions.serverUri.port}"

    val name = nodeOptions.name
    val regionFilter = nodeOptions.regionFilter
    val password = nodeOptions.password
    internal var cachedSession: Session? = null

    var sessionId: String? = nodeOptions.sessionId
        internal set

    internal val httpClient = OkHttpClient.Builder().callTimeout(nodeOptions.httpTimeout, TimeUnit.MILLISECONDS).build()

    internal val sink: Many<ClientEvent> = Sinks.many().multicast().onBackpressureBuffer()
    val flux: Flux<ClientEvent> = sink.asFlux()
    private val reference: Disposable = flux.subscribe()

    internal val rest = LavalinkRestClient(this)
    val ws = LavalinkSocket(this)

    // Stuff for load balancing
    val penalties = Penalties(this)
    var stats: Stats? = null
        internal set
    var available: Boolean = false
        internal set

    // TODO: Maybe it would make sense to add some node state enums like the links have.
    var transferring: Boolean = false
        internal set

    /**
     * A local player cache, allows us to not call the rest client every time we need a player.
     */
    internal val playerCache = ConcurrentHashMap<Long, LavalinkPlayer>()

    override fun dispose() {
        close()
    }

    override fun close() {
        available = false
        ws.close()
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
        httpClient.cache?.close()
        reference.dispose()
    }

    // For the java people
    /**
     * Listen to events from the node. Please note that uncaught exceptions will cause the listener to stop emitting events.
     *
     * @param type the [ClientEvent] to listen for
     *
     * @return a [Flux] of [ClientEvent]s
     */
    fun <T : ClientEvent> on(type: Class<T>): Flux<T> {
        return flux.ofType(type)
    }

    /**
     * Listen to events from the node. Please note that uncaught exceptions will cause the listener to stop emitting events.
     *
     * @return a [Flux] of [ClientEvent]s
     */
    inline fun <reified T : ClientEvent> on() = on(T::class.java)

    /**
     * Retrieves a list of all players from the lavalink node.
     *
     * @return A list of all players from the node.
     */
    fun getPlayers(): Mono<List<LavalinkPlayer>> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        return rest.getPlayers()
            .map { it.players.map { pl -> pl.toLavalinkPlayer(this) } }
            .doOnSuccess {
                it.forEach { player ->
                    playerCache[player.guildId] = player
                }
            }
    }

    /**
     * Gets the player from the guild id. If the player is not cached, it will be retrieved from the server.
     * If the player does not exist on the node, it will be created.
     *
     * @param guildId The guild id of the player.
     *
     * @Returns The player from the guild
     */
    fun getPlayer(guildId: Long): Mono<LavalinkPlayer> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        if (playerCache.containsKey(guildId)) {
            return playerCache[guildId].toMono()
        }

        return rest.getPlayer(guildId)
            .map { it.toLavalinkPlayer(this) }
            // Create the player if it doesn't exist on the node.
            .onErrorResume {
                if (it is RestException && it.code == 404) {
                    createOrUpdatePlayer(guildId)
                } else {
                    it.toMono()
                }
            }
            .doOnSuccess {
                // Update the player internally upon retrieving it.
                playerCache[it.guildId] = it
            }
    }

    fun updatePlayer(guildId: Long, updateConsumer: Consumer<PlayerUpdateBuilder>): Mono<LavalinkPlayer> {
        val update = createOrUpdatePlayer(guildId)

        updateConsumer.accept(update)

        return update
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
     * Destroy a guild's player and remove it from the cache. This will also remove the associated link from the client.
     *
     * @param guildId The guild id of the player AND link to destroy.
     */
    fun destroyPlayerAndLink(guildId: Long): Mono<Unit> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        return rest.destroyPlayer(guildId)
            .doOnSuccess {
                removeCachedPlayer(guildId)
                // When transferring to a new node preserve the link
                if (!transferring) {
                    lavalink.removeDestroyedLink(guildId)
                }
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
    fun loadItem(identifier: String): Mono<LavalinkLoadResult> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        return rest.loadItem(identifier).map { it.toLavalinkLoadResult() }
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
            .map { it.toCustom() }
    }

    /**
     * Uses the node to decode a list of base64 encoded tracks.
     *
     * @param encoded The base64 encoded tracks to decode.
     *
     * @return The decoded tracks.
     */
    fun decodeTracks(encoded: List<String>): Mono<List<Track>> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        return rest.decodeTracks(encoded)
            .map { it.tracks.map { track -> track.toCustom() } }
    }

    /**
     * Get information about the node.
     */
    fun getNodeInfo(): Mono<Info> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        return rest.getNodeInfo()
    }

    /**
     * Enables resuming. This causes Lavalink to continue playing for [timeout] amount of time, during which
     *  we can reconnect without losing our session data. */
    fun enableResuming(timeout: Duration): Mono<Session> {
        return rest.patchSession(Session(resuming = true, timeout.seconds)).doOnSuccess {
            cachedSession = it
        }
    }

    /**
     * Disables resuming, causing Lavalink to immediately drop all players upon this client disconnecting from it.
     *
     * This is the default behavior, reversing calls to [enableResuming].
     */
    fun disableResuming(): Mono<Session> {
        return rest.patchSession(Session(resuming = false, timeoutSeconds = 0)).doOnSuccess {
            cachedSession = it
        }
    }

    /**
     * Send a custom request to the lavalink node. Any host and port you set will be replaced with the node host automatically.
     * The scheme must match your node's scheme, however.
     *
     * It is recommended to use the path setter instead of the url setter when defining a url, like this:
     * <pre>{@code
     * customRequest((builder) -> {
     *     return builder.path("/some/plugin/path")
     *                   .get();
     * }).subscribe(System.out::println);
     * }</pre>
     *
     * @param builderFn The request builder function, defaults such as the Authorization header have already been applied
     *
     * @return The Http response from the node, may error with an IllegalStateException when the node is not available.
     */
    fun customRequest(builderFn: UnaryOperator<HttpBuilder>): Mono<Response> {
        if (!available) return Mono.error(IllegalStateException("Node is not available"))

        val call = rest.newRequest { builderFn.apply(this) }

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

    /**
     * Send a custom request to the lavalink node. Any host and port you set will be replaced with the node host automatically.
     * The scheme must match your node's scheme, however. The response body will be deserialized using the provided deserializer.
     *
     * It is recommended to use the path setter instead of the url setter when defining a url, like this:
     * <pre>{@code
     * customJsonRequest<SomeType>{
     *     it.path("/some/plugin/path")
     *                   .get();
     * }.doOnSuccess {
     *     if (it == null) {
     *        println("http 204");
     *     }
     *     println(it);
     * }.subscribe();
     * }</pre>
     *
     * @param builderFn The request builder function, defaults such as the Authorization header have already been applied
     *
     * @return The Json object from the response body, may error with an IllegalStateException when the node is not available or the response is not successful.
     */
    inline fun <reified T> customJsonRequest(builderFn: UnaryOperator<HttpBuilder>): Mono<T> =
        customJsonRequest(json.serializersModule.serializer<T>(), builderFn)

    /**
     * Send a custom request to the lavalink node. Any host and port you set will be replaced with the node host automatically.
     * The scheme must match your node's scheme, however. The response body will be deserialized using the provided deserializer.
     *
     * It is recommended to use the path setter instead of the url setter when defining a url, like this:
     * <pre>{@code
     * customJsonRequest(SomeType.Serializer.INSTANCE, (builder) -> {
     *     return builder.path("/some/plugin/path")
     *                   .get();
     * }).doOnSuccess((result) -> {
     *     if (result == null) {
     *        println("http 204");
     *     }
     *     println(result);
     * }).subscribe();
     * }</pre>
     *
     * @param deserializer The deserializer to use for the response body (E.G. `LoadResult.Serializer.INSTANCE`)
     * @param builderFn The request builder function, defaults such as the Authorization header have already been applied
     *
     * @return The Json object from the response body, may error with an IllegalStateException when the node is not available or the response is not successful.
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun <T> customJsonRequest(
        deserializer: DeserializationStrategy<T>,
        builderFn: UnaryOperator<HttpBuilder>
    ): Mono<T> {
        return customRequest(builderFn).flatMap { response ->
            response.use {
                if (!response.isSuccessful) {
                    val body = response.body!!.string()
                    if (body.isEmpty()) {
                        return@flatMap Mono.error(IllegalStateException("Request failed with code ${response.code}"))
                    }
                    json.decodeFromString<Error>(body).let { error ->
                        return@flatMap Mono.error(IllegalStateException("Request failed with code ${response.code} and message ${error.message}"))
                    }
                }

                if (response.code == 204) {
                    return@flatMap Mono.empty()
                }

                return@flatMap json.decodeFromStream(deserializer, response.body!!.byteStream())!!.toMono()
            }
        }
    }

    /**
     * Send a custom request to the lavalink node. Any host and port you set will be replaced with the node host automatically.
     * The scheme must match your node's scheme, however. The response body will be deserialized using the provided deserializer.
     *
     * It is recommended to use the path setter instead of the url setter when defining a url, like this:
     * <pre>{@code
     * customJsonRequest(SomeType.class, (builder) -> {
     *     return builder.path("/some/plugin/path")
     *                   .get();
     * }).doOnSuccess((result) -> {
     *     if (result == null) {
     *        println("http 204");
     *     }
     *     println(result);
     * }).subscribe();
     * }</pre>
     *
     * @param decodeTo The class that jackson will deserialize the response body into.
     * @param builderFn The request builder function, defaults such as the Authorization header have already been applied
     *
     * @return The Json object from the response body, may error with an IllegalStateException when the node is not available or the response is not successful.
     */
    fun <T> customJsonRequest(
        decodeTo: Class<T>,
        builderFn: UnaryOperator<HttpBuilder>
    ): Mono<T> {
        return customRequest(builderFn).flatMap { response ->
            response.use {
                if (!response.isSuccessful) {
                    val body = response.body!!.string()
                    if (body.isEmpty()) {
                        return@flatMap Mono.error(IllegalStateException("Request failed with code ${response.code}"))
                    }
                    json.decodeFromString<Error>(body).let { error ->
                        return@flatMap Mono.error(IllegalStateException("Request failed with code ${response.code} and message ${error.message}"))
                    }
                }

                if (response.code == 204) {
                    return@flatMap Mono.empty()
                }

                return@flatMap fromRawJson(response.body!!.byteStream(), decodeTo)!!.toMono()
            }
        }
    }

    /**
     * Get a [LavalinkPlayer] from the player cache.
     *
     * @return The cached player, or null if not yet cached.
     */
    fun getCachedPlayer(guildId: Long): LavalinkPlayer? = playerCache[guildId]

    internal fun getAndRemoveCachedPlayer(guildId: Long): LavalinkPlayer? = playerCache.remove(guildId)

    /**
     * @return an unmodifiable view of all cached players for this node.
     */
    fun getCachedPlayers(): Map<Long, LavalinkPlayer> = Collections.unmodifiableMap(playerCache)

    internal fun transferOrphansToSelf() {
        lavalink.transferOrphansTo(this)
    }

    internal fun synchronizeAfterResume() {
        getPlayers()
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(1)))
            .map { players ->
            val remoteGuildIds = players.map { it.guildId }

            players.forEach { player ->
                playerCache[player.guildId] = player

                val link = lavalink.getOrCreateLink(player.guildId, node = this)
                if (link.node != this) return@forEach

                link.state = if (player.state.connected) {
                    LinkState.CONNECTED
                } else {
                    LinkState.DISCONNECTED
                }
            }

            val missingIds = playerCache.keys().toList() - remoteGuildIds.toSet()
            missingIds.forEach { guildId ->
                playerCache.remove(guildId)
                val link = lavalink.getLinkIfCached(guildId) ?: return@forEach
                if (link.node == this) link.state = LinkState.DISCONNECTED
            }

            ResumeSynchronizationEvent(this, failureReason = null)
        }.doOnError {
            logger.error("Failure while attempting synchronization with $this", it)
            sink.tryEmitNext(ResumeSynchronizationEvent(this, failureReason = it))
        }.subscribe {
            sink.tryEmitNext(it)
        }
    }

    /**
     * Puts the node into a transferring state, then starts transferring all players to other nodes.
     * Once the node is put into this state, a reconnect or calling transferComplete() removes it from that state.
     * Transferring state does not remove links from cache when deleting off this node, unless the transfer also fails.
     */
    fun transferPlayersToOtherNodes() {
        transferring = true
        lavalink.transferNodes(this)
    }

    /**
     * Manually removes this node from the transfer state.
     */
    fun transferComplete() {
        transferring = false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LavalinkNode

        if (nodeOptions != other.nodeOptions) return false
        if (sessionId != other.sessionId) return false
        return available == other.available
    }

    override fun hashCode(): Int {
        var result = nodeOptions.hashCode()
        result = 31 * result + sessionId.hashCode()
        result = 31 * result + available.hashCode()
        return result
    }

    override fun toString(): String {
        return "LavalinkNode(name=$name, address=$baseUri, sessionId=$sessionId)"
    }
}
