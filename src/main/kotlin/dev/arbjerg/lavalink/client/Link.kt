package dev.arbjerg.lavalink.client

/**
 * A "Link" for linking a guild id to a node.
 * Mainly just a data class that contains some shortcuts to the node.
 * You should never store a link as it might be replaced internally without you knowing.
 */
class Link(
    val guildId: Long,
    initialNode: LavalinkNode
) {
    var node = initialNode
        internal set(newNode) {
            val player = node.getCachedPlayer(guildId)

            if (player != null) {
                PlayerUpdateBuilder(newNode.rest, guildId)
                    .setVoiceState(player.voiceState)
                    .asMono()
                    .block()
            }

            field = newNode
        }

    /**
     * Retrieves a list of all players from the lavalink server.
     */
    // Does it make sense to have getPlayers in the link?
    fun getPlayers() = node.getPlayers()

    /**
     * Gets the player for this link. If the player is not cached, it will be retrieved from the server.
     */
    fun getPlayer() = node.getPlayer(guildId)

    /**
     * Destroys the player for this link.
     */
    fun destroyPlayer() = node.destroyPlayer(guildId)

    /**
     * Load an item for the player.
     *
     * @param identifier The identifier (E.G. youtube url) to load.
     */
    fun loadItem(identifier: String) = node.loadItem(identifier)

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
