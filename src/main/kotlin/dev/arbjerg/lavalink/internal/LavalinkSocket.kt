package dev.arbjerg.lavalink.internal

import dev.arbjerg.lavalink.Version
import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.client.toClientEvent
import dev.arbjerg.lavalink.protocol.v4.Message
import dev.arbjerg.lavalink.protocol.v4.json
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.slf4j.LoggerFactory
import java.io.Closeable

class LavalinkSocket(private val node: LavalinkNode) : WebSocketListener(), Closeable {
    private val logger = LoggerFactory.getLogger(LavalinkSocket::class.java)

    internal var socket: WebSocket? = null

    var mayReconnect = true
    var lastReconnectAttempt = 0L
    var reconnectsAttempted = 0
    val reconnectInterval: Int
        get() = reconnectsAttempted * 2000 - 2000
    var open: Boolean = false
        private set

    init {
        connect()
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        logger.info("${node.name} has been connected!")
        node.available = true
        open = true
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val event = json.decodeFromString<Message>(text)

        when (event.op) {
            Message.Op.Ready -> {
                val sessionId = (event as Message.ReadyEvent).sessionId
                node.sessionId = sessionId
                logger.info("${node.name} is ready with session id $sessionId")
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
                node.penalties.handleTrackEvent(event as Message.EmittedEvent)
            }

            else -> {
                logger.error("Unknown WS message on ${node.name}, please report the following information to the devs: $text")
            }
        }

        // Handle user listeners when the lib has handled its own events.
        try {
            node.sink.tryEmitNext(event.toClientEvent(node))
        } catch (e: Exception) {
            node.sink.tryEmitError(e)
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        logger.info("${node.name} disconnected! (yell at devs to implement auto re-connect)")
        node.available = false
        open = false
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        logger.error("Unknown error on ${node.name}", t)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        node.available = false
        node.lavalink.onNodeDisconnected(node)

        if (code == 1000) {
            mayReconnect = false
            logger.info(
                "Connection to {}({}) closed normally with reason {} (closed by server = true)",
                node.name,
                webSocket.request().url,
                reason
            )
        } else {
            logger.info(
                "Connection to {}({}) closed abnormally with reason {}:{} (closed by server = true)",
                node.name,
                webSocket.request().url,
                code,
                reason
            )
        }

    }

    fun attemptReconnect() {
        lastReconnectAttempt = System.currentTimeMillis()
        reconnectsAttempted++
        connect()
    }

    private fun connect() {
        val request = Request.Builder()
            .url("${node.baseUri}/websocket")
            .addHeader("Authorization", node.password)
            .addHeader("Client-Name", "Lavalink-Client/${Version.VERSION}")
            .addHeader("User-Id", node.lavalink.userId.toString())
            .apply {
                if (node.sessionId != null) {
                    addHeader("Session-Id", node.sessionId!!)
                }
            }
            .build()

        try {
            socket = node.httpClient.newWebSocket(request, this)
            reconnectsAttempted = 0
        } catch (e: Exception) {
            logger.error("Failed to connect to WS of ${node.name} (${node.baseUri}), retrying in ${reconnectInterval / 1000} seconds", e)
        }
    }

    override fun close() {
        mayReconnect = false
        open = false
        socket?.cancel()
    }
}
