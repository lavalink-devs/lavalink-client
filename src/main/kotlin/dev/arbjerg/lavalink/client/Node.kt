package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.internal.ReusableWebSocket
import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.arbjerg.lavalink.protocol.v4.Message
import dev.arbjerg.lavalink.protocol.v4.Player
import dev.arbjerg.lavalink.protocol.v4.Players
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.java_websocket.drafts.Draft
import org.java_websocket.handshake.ServerHandshake
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.Many
import java.net.URI


class Node(serverUri: URI, draft: Draft, headers: Map<String, String>, connectTimeout: Int) :
    ReusableWebSocket(serverUri, draft, headers, connectTimeout), Disposable {

    private val sink: Many<Message.EmittedEvent> = Sinks.many().multicast().onBackpressureBuffer()
    val flux: Flux<Message.EmittedEvent> = sink.asFlux()
    private val reference: Disposable = flux.subscribe()

    override fun dispose() {
        reference.dispose()
    }

    fun <T : Message.EmittedEvent> on(type: Class<T>): Flux<T> {
        return flux.ofType(type)
    }

    fun getPlayers(): Mono<Players> {
        // GET /v4/sessions/{sessionId}/players
        TODO("Not yet implemented")
    }

    // TODO: where to store session id?
    fun getPlayer(guildId: String): Mono<Player?> {
        // GET /v4/sessions/{sessionId}/players/{guildId}
        // Keep track of players locally and create one if needed?
        TODO("Not yet implemented")
    }

    fun loadItem(identifier: String): Mono<LoadResult> {
        TODO("Not yet implemented")
    }

    override fun onOpen(handshakeData: ServerHandshake) {
        TODO("Not yet implemented")
    }

    override fun onMessage(raw: String) {
        val message = Json.decodeFromString<Message>(raw)

        when (message.op) {
            Message.Op.Ready -> {
                TODO("Not yet implemented")
            }
            Message.Op.Stats -> {
                TODO("Not yet implemented")
            }
            Message.Op.PlayerUpdate -> {
                TODO("Not yet implemented")
            }
            Message.Op.Event -> {
                try {
                    sink.tryEmitNext(message as Message.EmittedEvent)
                } catch (e: Exception) {
                    sink.tryEmitError(e)
                }
            }
        }
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onError(ex: Exception) {
        TODO("Not yet implemented")
    }
}
