package dev.arbjerg.lavalink.client.loadbalancing

import dev.arbjerg.lavalink.client.LavalinkNode

interface IRegionFilter {
    /** Whether this node may currently be used for this region */
    fun isRegionAllowed(node: LavalinkNode, region: VoiceRegion): RegionFilterVerdict
}

enum class RegionFilterVerdict {
    /** Allow this region to be used*/
    PASS,
    /** The load balancer should greatly prefer other nodes */
    SOFT_BLOCK,
    /** Do not use this node for this region under any circumstance */
    BLOCK
}

// TODO
//  - Class that has all discord voice servers
//  - Voice servers are being grouped into regions
//  - In case no exact server match it should look for the closest node in that same region
data class VoiceRegion(val id: String, val name: String)
