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
import java.net.URI
import java.util.UUID

class LavalinkNode(serverUri: URI, val userId: Long, val password: String) : Disposable {
    // "safe" uri with all paths aremoved
    val baseUri = "${serverUri.scheme}://${serverUri.host}:${serverUri.port}/v4"

    lateinit var sessionId: String

    private val sink: Many<Message.EmittedEvent> = Sinks.many().multicast().onBackpressureBuffer()
    val flux: Flux<Message.EmittedEvent> = sink.asFlux()
    private val reference: Disposable = flux.subscribe()

    private val rest = LavalinkRestClient(this)
    private val ws = LavalinkSocket(this, sink)

    override fun dispose() {
        reference.dispose()
    }

    // For the java people
    fun <T : Message.EmittedEvent> on(type: Class<T>): Flux<T> {
        return flux.ofType(type)
    }

    inline fun <reified T : Message.EmittedEvent> on() = on(T::class.java)

    // Rest methods
    fun getPlayers(): Mono<List<LavalinkPlayer>> = rest.getPlayers()
        .map { it.players.map { pl -> pl.toLavalinkPlayer(rest) } }

    fun createPlayer(guildId: Long): Mono<LavalinkPlayer> {
        return PlayerUpdateBuilder(rest, guildId)
            .setNoReplace(true)
            .asMono()
    }

    fun getPlayer(guildId: Long): Mono<LavalinkPlayer> {
        return rest.getPlayer(guildId)
            .map { it.toLavalinkPlayer(rest) }
    }

    fun destroyPlayer(guildId: Long): Mono<Unit> {
        return rest.destroyPlayer(guildId)
    }

    fun loadItem(identifier: String) = rest.loadItem(identifier)
}
