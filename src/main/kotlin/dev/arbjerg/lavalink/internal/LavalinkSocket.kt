package dev.arbjerg.lavalink.internal

import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketException
import com.neovisionaries.ws.client.WebSocketFrame
import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.protocol.v4.Message
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import reactor.core.publisher.Sinks

class LavalinkSocket(private val node: LavalinkNode, private val sink: Sinks.Many<Message.EmittedEvent>): WebSocketAdapter() {

    override fun onConnected(websocket: WebSocket, headers: MutableMap<String, MutableList<String>>) {
        //
    }

    override fun onTextMessage(websocket: WebSocket, text: String) {
        val message = Json.decodeFromString<Message>(text)

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
            else -> {
                TODO("Unknown OP")
            }
        }
    }

    override fun onCloseFrame(websocket: WebSocket, frame: WebSocketFrame) {
        //
    }

    override fun onError(websocket: WebSocket, cause: WebSocketException) {
        //
    }

}
