package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.player.LavalinkPlayer
import dev.arbjerg.lavalink.client.player.PlayerUpdateBuilder
import dev.arbjerg.lavalink.protocol.v4.VoiceState
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.function.Consumer
import kotlin.jvm.Volatile

/**
 * A "Link" for linking a guild id to a node.
 * Mainly just a data class that contains some shortcuts to the node.
 * You should never store a link as it might be replaced internally without you knowing.
 */
class Link(
    val guildId: Long,
    node: LavalinkNode
) {
    private var logger = LoggerFactory.getLogger(Link::class.java)

    var node = node
        private set

    /**
     * The voice connection state of this link
     */
    var state = LinkState.DISCONNECTED
        internal set

    /**
     * The timestamp in milliseconds of the last voice server update received for this link,
     * or `0` when no voice server update was received yet.
     *
     * This is used to detect whether the voice session was refreshed recently when a voice
     * WebSocket close event arrives, see [hasFreshVoiceSession].
     */
    @Volatile
    internal var lastVoiceUpdateMillis = 0L

    /**
     * Gets the player associated with this link. Returns null if it's not cached.
     */
    val cachedPlayer: LavalinkPlayer?
        get() = node.getCachedPlayer(guildId)

    /**
     * Gets the player for this link. If the player is not cached, it will be retrieved or created on the server.
     */
    fun getPlayer() = node.getPlayer(guildId)

    /**
     * Destroys this link, disconnecting the bot in the process.
     */
    fun destroy() = node.destroyPlayerAndLink(guildId)

    fun updatePlayer(updateConsumer: Consumer<PlayerUpdateBuilder>) = node.updatePlayer(guildId, updateConsumer)

    /**
     * Creates or updates the player for this link.
     */
    fun createOrUpdatePlayer() = node.createOrUpdatePlayer(guildId)

    /**
     * Load an item for the player.
     *
     * @param identifier The identifier (E.G. youtube url) to load.
     */
    fun loadItem(identifier: String) = node.loadItem(identifier)

    internal fun transferNode(newNode: LavalinkNode, delay: Duration = Duration.ZERO) {
        val player = node.getAndRemoveCachedPlayer(guildId)

        if (player != null) {
            state = LinkState.CONNECTING
            newNode.createOrUpdatePlayer(guildId)
                .applyBuilder(player.stateToBuilder())
                .delaySubscription(delay)
                .doOnError {
                    state = LinkState.DISCONNECTED
                    logger.error("Failed to transfer player to new node: ${newNode.name}", it)
                }.subscribe()
        } else {
            state = LinkState.DISCONNECTED
        }

        node = newNode
    }

    fun onVoiceServerUpdate(newVoiceState: VoiceState) {
        lastVoiceUpdateMillis = System.currentTimeMillis()

        if (node.available) {
            state = LinkState.CONNECTING
            node.createOrUpdatePlayer(guildId)
                .setVoiceState(newVoiceState)
                .subscribe(null) {
                    state = LinkState.DISCONNECTED
                    logger.error("Failed to update voice state to $newVoiceState", it)
                }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Link

        return guildId == other.guildId
    }

    override fun hashCode(): Int {
        return guildId.hashCode()
    }

    /**
     * Returns whether a voice server update was received for this link within
     * [VOICE_SESSION_REFRESH_WINDOW_MS] milliseconds before [nowMillis].
     *
     * Voice close code `4014` ("Disconnected") is also emitted when the bot is moved to another
     * voice channel, and the close event of the old session usually arrives after the voice
     * server update of the new session. A fresh voice update therefore means the close event is
     * stale and the player must not be destroyed, the new voice server update re-attaches it.
     */
    internal fun hasFreshVoiceSession(nowMillis: Long = System.currentTimeMillis()): Boolean =
        isVoiceSessionRefreshed(lastVoiceUpdateMillis, nowMillis)

    companion object {
        /**
         * The window in milliseconds in which a voice server update is considered a refreshed
         * voice session.
         */
        internal const val VOICE_SESSION_REFRESH_WINDOW_MS = 30_000L

        /**
         * Returns whether a voice session that last received a voice server update at
         * [lastVoiceUpdateMillis] counts as refreshed at [nowMillis].
         */
        internal fun isVoiceSessionRefreshed(lastVoiceUpdateMillis: Long, nowMillis: Long): Boolean =
            lastVoiceUpdateMillis > 0 && nowMillis - lastVoiceUpdateMillis < VOICE_SESSION_REFRESH_WINDOW_MS
    }
}
