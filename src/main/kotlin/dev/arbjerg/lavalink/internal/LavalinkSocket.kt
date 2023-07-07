package dev.arbjerg.lavalink.internal

import com.neovisionaries.ws.client.*
import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.protocol.v4.Message
import dev.arbjerg.lavalink.protocol.v4.json
import org.slf4j.LoggerFactory

// TODO: auto reconnect
class LavalinkSocket(private val node: LavalinkNode) : WebSocketAdapter() {
    private val logger = LoggerFactory.getLogger(LavalinkSocket::class.java)

    private val factory = WebSocketFactory()
    private var socket: WebSocket? = null

    init {
        connect()
    }

    override fun onConnected(websocket: WebSocket, headers: MutableMap<String, MutableList<String>>) {
        logger.info("Connected to Lavalink")
    }

    // TODO: emit these events from the client
    override fun onTextMessage(websocket: WebSocket, text: String) {
        val message = json.decodeFromString<Message>(text)

        when (message.op) {
            Message.Op.Ready -> {
                val sessionId = (message as Message.ReadyEvent).sessionId
                node.sessionId = sessionId
                logger.info("Lavalink is ready with session id $sessionId")
            }

            Message.Op.Stats -> {
                logger.debug("Stats are not implemented yet")
            }

            Message.Op.PlayerUpdate -> {
                val update = message as Message.PlayerUpdateEvent
                val idLong = update.guildId.toLong()

                if (idLong in node.playerCache) {
                    node.playerCache[idLong]!!.state = update.state
                }
            }

            Message.Op.Event -> {
                try {
                    node.sink.tryEmitNext(message as Message.EmittedEvent)
                } catch (e: Exception) {
                    node.sink.tryEmitError(e)
                }
            }

            else -> {
                logger.error("Unknown WS message, please report the following information to the devs: $text")
            }
        }
    }

    override fun onCloseFrame(websocket: WebSocket, frame: WebSocketFrame) {
        logger.info("Lavalink disconnected! (yell at devs to implement auto re-connect)")
    }

    override fun onError(websocket: WebSocket, cause: WebSocketException) {
        //
    }

    private fun connect() {
        socket = factory.createSocket("${node.baseUri}/websocket")

        socket!!.addListener(this)
            .setDirectTextMessage(false)
            .addHeader("Authorization", node.password)
            // TODO: fix version
            .addHeader("Client-Name", "Lavalink-Client/DEV")
            .addHeader("User-Id", node.userId.toString())
            .connect()
    }
}
