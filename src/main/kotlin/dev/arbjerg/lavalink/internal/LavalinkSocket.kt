package dev.arbjerg.lavalink.internal

import dev.arbjerg.lavalink.VERSION as CLIENT_VERSION
import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.client.LinkState
import dev.arbjerg.lavalink.client.protocol.toCustom
import dev.arbjerg.lavalink.client.toClientEvent
import dev.arbjerg.lavalink.protocol.v4.Message
import dev.arbjerg.lavalink.protocol.v4.json
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.slf4j.LoggerFactory
import reactor.core.publisher.Sinks
import java.io.Closeable
import java.io.EOFException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException

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
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        logger.debug("-> {}", text)

        val event = json.decodeFromString<Message>(text)

        when (event.op) {
            Message.Op.Ready -> {
                val sessionId = (event as Message.ReadyEvent).sessionId
                node.sessionId = sessionId
                logger.info("${node.name} is ready with session id $sessionId")

                node.playerCache.values.forEach { player ->
                    // Re-create the player on the node.
                    player.stateToBuilder()
                        .setNoReplace(false)
                        .subscribe()
                }
            }

            Message.Op.Stats -> {
                node.stats = (event as Message.StatsEvent)
            }

            Message.Op.PlayerUpdate -> {
                val update = event as Message.PlayerUpdateEvent
                val idLong = update.guildId.toLong()

                // Create a local player on the node if we don't have one.
                // There probably is an edge-case where this will happen.
                node.getOrAssumePlayer(idLong).state = update.state
                node.lavalink.getLinkIfCached(idLong)?.state = if (update.state.connected) {
                    LinkState.CONNECTED
                } else {
                    LinkState.DISCONNECTED
                }
            }

            Message.Op.Event -> {
                event as Message.EmittedEvent

                when (event) {
                    is Message.EmittedEvent.TrackStartEvent -> {
                        node.getCachedPlayer(event.guildId.toLong())?.track = event.track.toCustom()
                    }
                    is Message.EmittedEvent.TrackEndEvent -> {
                        node.getCachedPlayer(event.guildId.toLong())?.track = null
                    }
                    is Message.EmittedEvent.WebSocketClosedEvent -> {
                        // These codes represent an invalid session
                        // See https://discord.com/developers/docs/topics/opcodes-and-status-codes#voice-voice-close-event-codes

                        if (event.code == 4004 || event.code == 4006 || event.code == 4009 || event.code == 4014) {
                            logger.debug("Node '{}' received close code {} for guild {}", node.name, event.code, event.guildId)
                            // TODO: auto-reconnect?
                            node.destroyPlayerAndLink(event.guildId.toLong()).subscribe()
                        }
                    }
                    else -> {}
                }

                node.penalties.handleTrackEvent(event)
            }

            else -> {
                logger.error("Unknown WS message on ${node.name}, please report the following information to the devs: '$text'")
            }
        }

        // Handle user listeners when the lib has handled its own events.
        try {
            node.sink.tryEmitNext(event.toClientEvent(node))
        } catch (e: Exception) {
            node.sink.emitError(e, Sinks.EmitFailureHandler.FAIL_FAST)
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

            is SocketTimeoutException -> {
                logger.debug("Got disconnected from ${node.name} (timeout), trying to reconnect", t)
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
        if (socket != null) {
            socket?.close(1000, "New connection requested")
            socket?.cancel()
        }

        val request = Request.Builder()
            .url("${node.baseUri}/v4/websocket")
            .addHeader("Authorization", node.password)
            .addHeader("Client-Name", "Lavalink-Client/${CLIENT_VERSION}")
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
