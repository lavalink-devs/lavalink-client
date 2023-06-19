package dev.arbjerg.lavalink.internal

import com.neovisionaries.ws.client.*
import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.protocol.v4.Message
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import reactor.core.publisher.Sinks

class LavalinkSocket(private val node: LavalinkNode, private val sink: Sinks.Many<Message.EmittedEvent>) :
    WebSocketAdapter() {

    private val factory = WebSocketFactory()
    private var socket: WebSocket? = null

    init {
        connect()
    }

    override fun onConnected(websocket: WebSocket, headers: MutableMap<String, MutableList<String>>) {
        println("Connected to Lavalink Node")
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

    private fun connect() {
        socket = factory.createSocket("${node.baseUri}/websocket")

        socket!!.addListener(this)
            .setDirectTextMessage(true)
            .addHeader("Authorization", node.password)
            // TODO: fix version
            .addHeader("Client-Name", "Lavalink-Client/DEV")
            .addHeader("User-Id", node.userId.toString())
            .connect()
    }
}
