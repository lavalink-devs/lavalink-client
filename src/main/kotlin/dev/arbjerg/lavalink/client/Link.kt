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
    var node = initialNode
        /*get () {
            if (!field.available) {
                throw IllegalStateException("Node is not available")
            }

            return field
        }*/
        internal set(newNode) {
            val player = getCachedPlayer()

            if (player != null) {
                PlayerUpdateBuilder(newNode.rest, guildId)
                    .setVoiceState(player.voiceState)
                    .asMono()
                    .block()
            }

            field = newNode
        }

    fun getPlayers() = node.getPlayers()
    fun getCachedPlayer() = node.getCachedPlayer(guildId)
    fun getPlayer() = node.getPlayer(guildId)
    fun destroyPlayer() = node.destroyPlayer(guildId)
    fun loadItem(identifier: String) = node.loadItem(identifier)
}
