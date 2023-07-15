package dev.arbjerg.lavalink.internal

import com.neovisionaries.ws.client.*
import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.Version
import dev.arbjerg.lavalink.protocol.v4.Message
import dev.arbjerg.lavalink.protocol.v4.json
import org.slf4j.LoggerFactory
import java.io.Closeable

// TODO: auto reconnect
class LavalinkSocket(private val node: LavalinkNode) : WebSocketAdapter(), Closeable {
    private val logger = LoggerFactory.getLogger(LavalinkSocket::class.java)

    private val factory = WebSocketFactory()
    internal var socket: WebSocket? = null

    var mayReconnect = true
    var lastReconnectAttempt = 0L
    var reconnectsAttempted = 0
    val reconnectInterval: Int
        get() = reconnectsAttempted * 2000 - 2000

    init {
        connect()
    }

    override fun onConnected(websocket: WebSocket, headers: MutableMap<String, MutableList<String>>) {
        logger.info("Connected to Lavalink")
        node.available = true
    }

    override fun onTextMessage(websocket: WebSocket, text: String) {
        val event = json.decodeFromString<Message>(text)

        when (event.op) {
            Message.Op.Ready -> {
                val sessionId = (event as Message.ReadyEvent).sessionId
                node.sessionId = sessionId
                logger.info("Lavalink is ready with session id $sessionId")
            }

            Message.Op.Stats -> {
                node.stats = (event as Message.StatsEvent)
            }

            Message.Op.PlayerUpdate -> {
                val update = event as Message.PlayerUpdateEvent
                val idLong = update.guildId.toLong()

                if (idLong in node.playerCache) {
                    node.playerCache[idLong]!!.state = update.state
                }
            }

            Message.Op.Event -> {
                try {
                    val eventTyped = event as Message.EmittedEvent

                    node.penalties.handleTrackEvent(eventTyped)
                    node.sink.tryEmitNext(eventTyped)
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
        node.available = false
    }

    override fun onError(websocket: WebSocket, cause: WebSocketException) {
        logger.error("Unknown error", cause)
    }

    override fun onDisconnected(
        websocket: WebSocket,
        serverCloseFrame: WebSocketFrame?,
        clientCloseFrame: WebSocketFrame?,
        closedByServer: Boolean
    ) {
        node.available = false

        if (closedByServer && serverCloseFrame != null) {
            val reason = serverCloseFrame.closeReason ?: "<no reason given>"
            val code = serverCloseFrame.closeCode

            if (code == WebSocketCloseCode.NORMAL) {
                mayReconnect = false
                logger.info("Connection to {} closed normally with reason {} (closed by server = true)", websocket.uri, reason)
            } else {
                logger.info("Connection to {} closed abnormally with reason {}:{} (closed by server = true)", websocket.uri, code, reason)
            }

            return
        }

        if (clientCloseFrame != null) {
            val code = clientCloseFrame.closeCode
            val reason = clientCloseFrame.closeReason ?: "<no reason given>"

            if (code == WebSocketCloseCode.NORMAL) {
                mayReconnect = false
            }

            logger.info("Connection to {} closed by client with code {} and reason {} (closed by server = false)", websocket.uri, code, reason)
        }
    }

    fun attemptReconnect() {
        lastReconnectAttempt = System.currentTimeMillis()
        reconnectsAttempted++
        connect()
    }

    private fun connect() {
        socket = factory.createSocket("${node.baseUri}/websocket")
            .addListener(this)
            .setDirectTextMessage(false)
            .addHeader("Authorization", node.password)
            .addHeader("Client-Name", "Lavalink-Client/${Version.VERSION}")
            .addHeader("User-Id", node.lavalink.userId.toString())

        try {
            socket!!.connect()
            reconnectsAttempted = 0
        } catch (e: Exception) {
            logger.error("Failed to connect to WS of node '${node.name}', retrying in ${reconnectInterval / 1000} seconds", e)
        }
    }

    override fun close() {
        socket?.disconnect()
    }
}
