package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.player.LavalinkPlayer
import dev.arbjerg.lavalink.client.player.PlayerUpdateBuilder
import dev.arbjerg.lavalink.protocol.v4.VoiceState
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.function.Consumer

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
}
