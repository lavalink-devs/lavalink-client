package dev.arbjerg.lavalink.client

/**
 * A "Link" for linking a guild id to a node.
 * Mainly just a data class that contains some shortcuts to the node.
 * You should never store a link as it might be replaced internally without you knowing.
 */
data class Link(
    val guildId: Long,
    val initialNode: LavalinkNode
) {
    // TODO: actual change node function to also handle server updates.
    var node = initialNode
        internal set

    fun getPlayers() = node.getPlayers()
    fun getPlayer() = node.getPlayer(guildId)
    fun destroyPlayer() = node.destroyPlayer(guildId)
    fun loadItem(identifier: String) = node.loadItem(identifier)
}
