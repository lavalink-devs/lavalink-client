package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.internal.LavalinkRestClient
import dev.arbjerg.lavalink.internal.LavalinkSocket
import dev.arbjerg.lavalink.internal.toLavalinkPlayer
import dev.arbjerg.lavalink.protocol.v4.Message
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.Many
import reactor.kotlin.core.publisher.toMono
import java.net.URI

class LavalinkNode(serverUri: URI, val userId: Long, val password: String) : Disposable {
    // "safe" uri with all paths aremoved
    val baseUri = "${serverUri.scheme}://${serverUri.host}:${serverUri.port}/v4"

    lateinit var sessionId: String

    internal val sink: Many<Message.EmittedEvent> = Sinks.many().multicast().onBackpressureBuffer()
    val flux: Flux<Message.EmittedEvent> = sink.asFlux()
    private val reference: Disposable = flux.subscribe()

    val rest = LavalinkRestClient(this)
    val ws = LavalinkSocket(this)

    /**
     * A local player cache, allows us to not call the rest client every time we need a player.
     */
    internal val playerCache = mutableMapOf<Long, LavalinkPlayer>()

    override fun dispose() {
        reference.dispose()
    }

    // For the java people
    fun <T : Message.EmittedEvent> on(type: Class<T>): Flux<T> {
        return flux.ofType(type)
    }

    inline fun <reified T : Message.EmittedEvent> on() = on(T::class.java)

    // Rest methods
    fun getPlayers(): Mono<List<LavalinkPlayer>> {
        if (playerCache.isEmpty()) {
            return rest.getPlayers()
                .map { it.players.map { pl -> pl.toLavalinkPlayer(rest) } }
                .doOnNext {
                    it.forEach { player ->
                        playerCache[player.guildId] = player
                    }
                }
        }

        return playerCache.values.toList().toMono()
    }

    fun getPlayer(guildId: Long): Mono<LavalinkPlayer> {
        if (guildId in playerCache) {
            return playerCache[guildId].toMono()
        }

        return rest.getPlayer(guildId)
            .map { it.toLavalinkPlayer(rest) }
            // Update the player internally upon retrieving it.
            .doOnNext {
                playerCache[it.guildId] = it
            }
    }

    fun destroyPlayer(guildId: Long): Mono<Unit> {
        return rest.destroyPlayer(guildId)
            .doOnNext {
                playerCache.remove(guildId)
            }
    }

    fun loadItem(identifier: String) = rest.loadItem(identifier)
}
