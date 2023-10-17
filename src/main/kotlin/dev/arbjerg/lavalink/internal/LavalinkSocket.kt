package dev.arbjerg.lavalink.internal

import dev.arbjerg.lavalink.LLClientInfo
import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.client.LinkState
import dev.arbjerg.lavalink.client.toClientEvent
import dev.arbjerg.lavalink.protocol.v4.Message
import dev.arbjerg.lavalink.protocol.v4.json
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.EOFException
import java.net.ConnectException
import java.net.SocketException

class LavalinkSocket(private val node: LavalinkNode) : WebSocketListener(), Closeable {
    private val logger = LoggerFactory.getLogger(LavalinkSocket::class.java)

    internal var socket: WebSocket? = null

    var mayReconnect = true
    var lastReconnectAttempt = 0L
    private var reconnectsAttempted = 0
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
        reconnectsAttempted = 0

        node.playerCache.values.forEach { player ->
            // Re-create the player on the node.
            player.stateToBuilder()
                .setNoReplace(false)
                .asMono().subscribe()
        }
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
                    node.lavalink.getLinkIfCached(idLong)?.state = if (update.state.connected) {
                        LinkState.CONNECTED
                    } else {
                        LinkState.DISCONNECTED
                    }
                }
            }

            Message.Op.Event -> {
                // TODO: handle websocket closed event from discord?
                node.penalties.handleTrackEvent(event as Message.EmittedEvent)
            }

            else -> {
                logger.error("Unknown WS message on ${node.name}, please report the following information to the devs: '$text'")
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
        if (mayReconnect) {
            logger.info("${node.name} disconnected, reconnecting in ${reconnectInterval / 1000} seconds")
            node.available = false
            open = false
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        node.lavalink.onNodeDisconnected(node)

        when(t) {
            is EOFException -> {
                logger.debug("Got disconnected from ${node.name}, trying to reconnect", t)
                node.available = false
                open = false
            }

            is ConnectException -> {
                logger.error("Failed to connect to WS of ${node.name} (${node.baseUri}), retrying in ${reconnectInterval / 1000} seconds", t)
            }

            is SocketException -> {
                if (!open) {
                    // We closed the connection, ty okhttp for informing us.
                    logger.debug("Got a socket exception on ${node.name}, but the socket is closed. Ignoring it", t)
                    return
                }

                logger.error("Socket error on ${node.name}, reconnecting in ${reconnectInterval / 1000} seconds", t)
                node.available = false
                open = false
            }

            else -> {
                logger.error("Unknown error on ${node.name}", t)
            }
        }
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
            .url("${node.baseUri}/v4/websocket")
            .addHeader("Authorization", node.password)
            .addHeader("Client-Name", "Lavalink-Client/${LLClientInfo.VERSION}")
            .addHeader("User-Id", node.lavalink.userId.toString())
            .apply {
                if (node.sessionId != null) {
                    addHeader("Session-Id", node.sessionId!!)
                }
            }
            .build()

        socket = node.httpClient.newWebSocket(request, this)
    }

    override fun close() {
        mayReconnect = false
        open = false
        socket?.close(1000, "Client shutdown")
        socket?.cancel()
    }
}
