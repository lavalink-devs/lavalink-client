package dev.arbjerg.lavalink.internal

import dev.arbjerg.lavalink.client.LavalinkNode

/**
 * A "Link" for linking a guild id to a node.
 * Mainly just a data class.
 */
data class Link(
    val guildId: Long,
    val node: LavalinkNode
) {
    fun getPlayer() = node.getPlayer(guildId)
}
