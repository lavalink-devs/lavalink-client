package dev.arbjerg.lavalink.internal

import dev.arbjerg.lavalink.client.LavalinkNode

/**
 * A "Link" for linking a guild id to a node.
 * Mainly just a data class that contains some shortcuts to the node.
 */
class Link(
    val guildId: Long,
    val node: LavalinkNode
) {
    fun getPlayers() = node.getPlayers()
    fun getPlayer() = node.getPlayer(guildId)
    fun createPlayer() = node.createPlayer(guildId)
    fun destroyPlayer() = node.destroyPlayer(guildId)
    fun loadItem(identifier: String) = node.loadItem(identifier)
}
