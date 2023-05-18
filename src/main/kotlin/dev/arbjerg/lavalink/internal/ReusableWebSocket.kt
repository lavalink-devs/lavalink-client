package dev.arbjerg.lavalink.internal

import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft
import org.java_websocket.handshake.ServerHandshake
import java.net.InetSocketAddress
import java.net.URI


// Copied from https://github.com/freyacodes/Lavalink-Client/blob/master/src/main/java/lavalink/client/io/ReusableWebSocket.java
// Not sure if I want to keep this
abstract class ReusableWebSocket(
    private val serverUri: URI,
    private val draft: Draft,
    private val headers: Map<String, String>,
    private val connectTimeout: Int
) {

    private var socket: DisposableSocket? = null

    var isUsed = false
    private val instance   // For use in inner class
        get() = this

    private var heartbeatTimeout = 60

    abstract fun onOpen(handshakeData: ServerHandshake)
    abstract fun onMessage(raw: String)
    abstract fun onClose(code: Int, reason: String, remote: Boolean)
    abstract fun onError(ex: Exception)

    // TODO: sending is not supported in v4
    fun send(text: String) {
        val localSocket = socket

        if (localSocket != null && localSocket.isOpen) {
            localSocket.send(text)
        }
    }

    fun getServerUri(): URI {
        return serverUri
    }

    // will return null if there is no connection
    fun getRemoteSocketAddress(): InetSocketAddress? {
        return socket?.remoteSocketAddress
    }

    fun isOpen(): Boolean {
        val localSocket = socket
        return localSocket != null && localSocket.isOpen
    }

    fun isConnecting(): Boolean {
        val localSocket = socket
        return localSocket != null && !localSocket.isOpen && !localSocket.isClosed && !localSocket.isClosing
    }

    fun isClosed(): Boolean {
        val localSocket = socket
        return localSocket == null || localSocket.isClosed
    }

    fun isClosing(): Boolean {
        val localSocket = socket
        return localSocket != null && localSocket.isClosing
    }

    fun connect() {
        if (socket == null || isUsed) {
            socket = DisposableSocket(serverUri, draft, headers, connectTimeout)
        }

        socket?.connectionLostTimeout = heartbeatTimeout
        socket?.connect()
        isUsed = true
    }

    fun close() {
        socket?.close()
    }

    fun close(code: Int) {
        socket?.close(code)
    }

    fun close(code: Int, reason: String?) {
        socket?.close(code, reason)
    }

    fun setHeartbeatTimeout(seconds: Int) {
        heartbeatTimeout = seconds
        socket?.connectionLostTimeout = seconds
    }

    inner class DisposableSocket internal constructor(
        serverUri: URI,
        protocolDraft: Draft,
        httpHeaders: Map<String, String>,
        connectTimeout: Int
    ) :
        WebSocketClient(serverUri, protocolDraft, httpHeaders, connectTimeout) {
        init {
            isUsed = false
        }

        override fun onOpen(handshakedata: ServerHandshake) {
            instance.onOpen(handshakedata)
        }

        override fun onMessage(message: String) {
            instance.onMessage(message)
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            instance.onClose(code, reason, remote)
        }

        override fun onError(ex: Exception) {
            instance.onError(ex)
        }
    }

}
