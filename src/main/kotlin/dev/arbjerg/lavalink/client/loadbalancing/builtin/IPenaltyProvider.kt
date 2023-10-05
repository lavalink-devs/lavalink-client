package dev.arbjerg.lavalink.client.loadbalancing.builtin

import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.client.loadbalancing.VoiceRegion
import dev.arbjerg.lavalink.client.loadbalancing.MAX_ERROR

fun interface IPenaltyProvider {
    /**
     * Calculate the penalty for the provider.
     *
     * Return value should never exceed [MAX_ERROR]. Lower means to take preference.
     *
     * @param node The lavalink node to calculate the penalty for.
     * @param region The preferred voice region for the node, null if not specified.
     *
     * @return A number between 0 and [MAX_ERROR] (inclusive), using numbers outside of this range may cause errors.
     */
    fun getPenalty(node: LavalinkNode, region: VoiceRegion?): Int
}
