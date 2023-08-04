package dev.arbjerg.lavalink.internal

import dev.arbjerg.lavalink.client.LavalinkClient
import org.slf4j.LoggerFactory

class ReconnectTask(val lavalink: LavalinkClient) : Runnable {
    private val logger = LoggerFactory.getLogger(ReconnectTask::class.java)

    override fun run() {
        // We only need access to the sockets in this instances
        val sockets = lavalink.nodes.map { it.ws }.filter { it.socket != null }

        try {
            sockets.forEach { ws ->
                if (
                    !ws.open &&
                    System.currentTimeMillis() - ws.lastReconnectAttempt > ws.reconnectInterval &&
                    ws.mayReconnect
                ) {
                    ws.attemptReconnect()
                }
            }
        } catch (e: Exception) {
            logger.error("Error while reconnecting a node", e)
        }
    }
}
