package dev.arbjerg.lavalink.client.loadbalancing.builtin

import dev.arbjerg.lavalink.client.LavalinkNode
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
            RegionFilterVerdict.SOFT_BLOCK -> 100000
            RegionFilterVerdict.BLOCK -> Integer.MAX_VALUE - 1
        }
    }
}
