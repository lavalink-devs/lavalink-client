package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.internal.LavalinkRestClient
import dev.arbjerg.lavalink.internal.LavalinkSocket
import dev.arbjerg.lavalink.internal.toLavalinkPlayer
import dev.arbjerg.lavalink.protocol.v4.Message
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.Many
import java.net.URI


class LavalinkNode(serverUri: URI) : Disposable {
    // "safe" uri with all paths aremoved
    val baseUri = URI.create("${serverUri.scheme}://${serverUri.host}:${serverUri.port}")

    val sessionId: String = TODO("Not yet implemented")

    private val sink: Many<Message.EmittedEvent> = Sinks.many().multicast().onBackpressureBuffer()
    val flux: Flux<Message.EmittedEvent> = sink.asFlux()
    private val reference: Disposable = flux.subscribe()

    private val rest = LavalinkRestClient(this)
    private val ws = LavalinkSocket(this, sink)

    init {
        // TODO: do we want to connect on initialisation?
    }

    override fun dispose() {
        reference.dispose()
    }

    // For the java people
    fun <T : Message.EmittedEvent> on(type: Class<T>): Flux<T> {
        return flux.ofType(type)
    }

    inline fun <reified T : Message.EmittedEvent> on() = on(T::class.java)

    // Rest methods
    fun getPlayers() = rest.getPlayers()
        .map { it.players.map { pl -> pl.toLavalinkPlayer(rest) } }

    fun getPlayer(guildId: Long) = rest.getPlayer(guildId)
        .map { it?.toLavalinkPlayer(rest) }

    fun loadItem(identifier: String) = rest.loadItem(identifier)
}
