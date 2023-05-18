package dev.arbjerg.lavalink.internal

import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.protocol.v4.Message
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import reactor.core.publisher.Sinks

class LavalinkSocket(private val node: LavalinkNode, private val sink: Sinks.Many<Message.EmittedEvent>) {

    fun onOpen() {
        TODO("Not yet implemented")
    }

    fun onMessage(raw: String) {
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

    fun onClose(code: Int, reason: String, remote: Boolean) {
        TODO("Not yet implemented")
    }

    fun onError(ex: Exception) {
        TODO("Not yet implemented")
    }

}
