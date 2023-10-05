package dev.arbjerg.lavalink.client.loadbalancing.builtin

import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.client.loadbalancing.MAX_ERROR
import dev.arbjerg.lavalink.client.loadbalancing.RegionFilterVerdict
import dev.arbjerg.lavalink.client.loadbalancing.VoiceRegion

class VoiceRegionPenaltyProvider : IPenaltyProvider {
    override fun getPenalty(node: LavalinkNode, region: VoiceRegion?): Int {
        val filter = node.regionFilter

        if (region == null || filter == null) {
            return 0
        }

        val verdict = filter.isRegionAllowed(node, region)

        return when (verdict) {
            RegionFilterVerdict.PASS -> 0
            RegionFilterVerdict.SOFT_BLOCK -> 1000
            RegionFilterVerdict.BLOCK -> MAX_ERROR
        }
    }
}
