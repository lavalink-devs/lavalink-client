package dev.arbjerg.lavalink.client.loadbalancing.builtin

import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.client.loadbalancing.VoiceRegion

class VoiceRegionPenaltyProvider : IPenaltyProvider {
    override fun getPenalty(node: LavalinkNode, region: VoiceRegion): Int {
        TODO("Not yet implemented")
    }
}
