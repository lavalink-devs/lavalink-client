package dev.arbjerg.lavalink.client.loadbalancing.builtin

import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.client.loadbalancing.VoiceRegion

fun interface IPenaltyProvider {
    fun getPenalty(node: LavalinkNode, region: VoiceRegion): Int
}
