package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.protocol.v4.VoiceState

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
     * Gets the player for this link. If the player is not cached, it will be retrieved from the server.
     */
    fun getPlayer() = node.getPlayer(guildId)

    /**
     * Destroys the player for this link.
     */
    fun destroyPlayer() = node.destroyPlayer(guildId)

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
                .setVoiceState(player.voiceState)
                .asMono()
                .block()
        }

        node = newNode
    }

    fun onVoiceServerUpdate(newVoiceState: VoiceState) {
        if (node.available) {
            node.createOrUpdatePlayer(guildId)
                .setVoiceState(newVoiceState)
                .asMono()
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
