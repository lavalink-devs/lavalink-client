package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.protocol.v4.VoiceState
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
    var node = node
        private set

    /**
     * The voice connection state of this link
     */
    var state = LinkState.DISCONNECTED
        internal set

    /**
     * Gets the player for this link. If the player is not cached, it will be retrieved or created on the server.
     *
     * If you just want a local player instead, you can use [getOrCreateCachedPlayer]
     */
    fun getPlayer() = node.getPlayer(guildId)

    /**
     * Gets the cached player for this link. If the player is not cached it will be created locally.
     *
     * To ensure a player also exist on the node, you can use [getPlayer]
     */
    fun getOrCreateCachedPlayer() = node.getOrCreateCachedPlayer(guildId)

    /**
     * Destroys the player for this link. This will also remove the link from the client.
     */
    fun destroyPlayer() = node.destroyPlayer(guildId)

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

    internal fun transferNode(newNode: LavalinkNode) {
        val player = node.getCachedPlayer(guildId)

        if (player != null) {
            node.removeCachedPlayer(guildId)
            newNode.createOrUpdatePlayer(guildId)
                .applyBuilder(player.stateToBuilder())
                .subscribe()
        }

        node = newNode
    }

    fun onVoiceServerUpdate(newVoiceState: VoiceState) {
        if (node.available) {
            state = LinkState.CONNECTING
            node.createOrUpdatePlayer(guildId)
                .setVoiceState(newVoiceState)
                .subscribe()
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
