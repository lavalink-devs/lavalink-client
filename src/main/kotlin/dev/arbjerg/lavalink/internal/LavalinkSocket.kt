package dev.arbjerg.lavalink.internal

import dev.arbjerg.lavalink.VERSION as CLIENT_VERSION
import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.client.LinkState
import dev.arbjerg.lavalink.client.player.toCustom
import dev.arbjerg.lavalink.client.event.toClientEvent
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
import java.util.concurrent.atomic.AtomicBoolean

class LavalinkSocket(private val node: LavalinkNode) : WebSocketListener(), Closeable {
    private val logger = LoggerFactory.getLogger(LavalinkSocket::class.java)

    internal var socket: WebSocket? = null

    var mayReconnect = true
    var lastReconnectAttempt = 0L
    @Volatile
    private var reconnectsAttempted = 0
    val reconnectInterval: Int
        get() = reconnectsAttempted * 2000 - 2000
    var open: Boolean = false
        private set
    @Volatile
    private var hasEverConnected = false
    private val isAttemptingResume = AtomicBoolean(node.sessionId != null)

    init {
        connect()
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        logger.info("${node.name} has been connected!")
        open = true
        reconnectsAttempted = 0
        hasEverConnected = true
        isAttemptingResume.set(false)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        logger.debug("-> {}", text)

        val event = json.decodeFromString<Message>(text)

        when (event.op) {
            Message.Op.Ready -> {
                val (_, resumed, sessionId) = event as Message.ReadyEvent

                // in case this was a reboot of the node, and we are not resuming
                // reset the metrics to prevent the loadbalancer not liking this node
                if (!resumed) {
                    node.penalties.resetMetrics()
                }

                node.sessionId = sessionId
                node.available = true
                node.transferring = false
                logger.info("${node.name} is ready with session id $sessionId")

                node.playerCache.values.forEach { player ->
                    // Ignore empty voice states, not sure what causes this
                    val (token, endpoint, stateSessionId) = player.voiceState

                    if (token.isBlank() || endpoint.isBlank() || stateSessionId.isBlank()) {
                        return@forEach
                    }

                    // Re-create the player on the node.
                    player.stateToBuilder()
                        .setNoReplace(false)
                        .subscribe()
                }

                if (!resumed) {
                    node.cachedSession = null
                }
                if (node.cachedSession == null) {
                    node.rest.getSession().subscribe { node.cachedSession = it }
                }

                if (resumed) {
                    node.synchronizeAfterResume()
                }

                // Move players from older, unavailable nodes to ourselves.
                node.transferOrphansToSelf()
            }

            Message.Op.Stats -> {
                node.stats = (event as Message.StatsEvent)
            }

            Message.Op.PlayerUpdate -> {
                val update = event as Message.PlayerUpdateEvent
                val idLong = update.guildId.toLong()

                node.getCachedPlayer(idLong)?.state = update.state
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
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        handleFailureThrowable(t)

        node.available = false
        open = false

        node.lavalink.onNodeDisconnected(node)
    }

    private fun handleFailureThrowable(t: Throwable) {
        when(t) {
            is EOFException -> {
                logger.debug("Got disconnected from ${node.name}, trying to reconnect", t)
            }

            is SocketTimeoutException -> {
                logger.debug("Got disconnected from ${node.name} (timeout), trying to reconnect", t)
            }

            is ConnectException -> {
                logger.warnOrTrace("Failed to connect to WS of ${node.name} (${node.baseUri}), retrying in ${reconnectInterval / 1000} seconds", t)
            }

            is SocketException -> {
                if (!open) {
                    // We closed the connection, ty okhttp for informing us.
                    logger.debug("Got a socket exception on ${node.name}, but the socket is closed. Ignoring it", t)
                    return
                }

                logger.warnOrTrace("Socket error on ${node.name}, reconnecting in ${reconnectInterval / 1000} seconds", t)
            }

            else -> {
                logger.error("Unknown error on ${node.name}", t)
            }
        }

        if (hasEverConnected && isAttemptingResume.getAndSet(false)) {
            try {
                node.lavalink.onResumeReconnectFailed(node)
            } catch (e: Exception) {
                logger.error("Exception after giving up on resuming", e)
            }
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        node.available = false
        open = false
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
                if (node.sessionId != null && isAttemptingResume.get()) {
                    addHeader("Session-Id", node.sessionId!!)
                }
            }
            .build()

        mayReconnect = true
        socket = node.httpClient.newWebSocket(request, this)
    }

    override fun close() {
        mayReconnect = false
        open = false
        node.available = false
        socket?.close(1000, "Client shutdown")
        socket?.cancel()
    }

    internal fun onResumableConnectionDisconnected() {
        isAttemptingResume.set(true)
    }
}
