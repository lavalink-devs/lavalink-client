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

/**
 * Custom [IRegionFilter] that groups voice regions together.
 * You must register [dev.arbjerg.lavalink.client.loadbalancing.builtin.VoiceRegionPenaltyProvider] as a penalty provider in order for this filter to work.
 */
object RegionGroup {
    @JvmField
    val ASIA: IRegionFilter = object : IRegionFilter {
        val regions = listOf(VoiceRegion.SIDNEY, VoiceRegion.INDIA, VoiceRegion.JAPAN, VoiceRegion.HONGKONG, VoiceRegion.SOUTH_AFRICA)

        override fun isRegionAllowed(node: LavalinkNode, region: VoiceRegion): RegionFilterVerdict {
            return if (region in regions) RegionFilterVerdict.PASS else RegionFilterVerdict.SOFT_BLOCK
        }
    }
    @JvmField
    val EUROPE: IRegionFilter = object : IRegionFilter {
        val regions = listOf(VoiceRegion.ROTTERDAM, VoiceRegion.RUSSIA)

        override fun isRegionAllowed(node: LavalinkNode, region: VoiceRegion): RegionFilterVerdict {
            return if (region in regions) RegionFilterVerdict.PASS else RegionFilterVerdict.SOFT_BLOCK
        }
    }
    @JvmField
    val US: IRegionFilter = object : IRegionFilter {
        val regions = listOf(VoiceRegion.BRAZIL, VoiceRegion.SIDNEY, VoiceRegion.US_CENTRAL, VoiceRegion.US_EAST, VoiceRegion.US_SOUTH, VoiceRegion.US_WEST)

        override fun isRegionAllowed(node: LavalinkNode, region: VoiceRegion): RegionFilterVerdict {
            return if (region in regions) RegionFilterVerdict.PASS else RegionFilterVerdict.SOFT_BLOCK
        }
    }
}

// TODO
//  - Class that has all discord voice servers
//  - Voice servers are being grouped into regions
//  - In case no exact server match it should look for the closest node in that same region
enum class VoiceRegion(val id: String, val visibleName: String) {
    BRAZIL("brazil", "Brazil"),
    HONGKONG("hongkong", "Hong Kong"),
    INDIA("india", "India"),
    JAPAN("japan", "Japan"),
    ROTTERDAM("rotterdam", "Rotterdam"),
    RUSSIA("russia", "Russia"),
    SINGAPORE("singapore", "Singapore"),
    SOUTH_AFRICA("southafrica", "South Africa"),
    SIDNEY("sidney", "Sidney"),
    US_CENTRAL("us-central", "US Central"),
    US_EAST("us-east", "US East"),
    US_SOUTH("us-south", "US South"),
    US_WEST("us-west", "US West"),

    UNKNOWN("", "Unknown");

    companion object {
        @JvmStatic
        fun fromEndpoint(endpoint: String): VoiceRegion {
            val endpointRegex = "^([a-z\\-]+)[0-9]+.*:443\$".toRegex()
            val match = endpointRegex.find(endpoint) ?: return UNKNOWN
            val idFromEndpoint = match.groupValues[1]

            return entries.find { it.id == idFromEndpoint } ?: UNKNOWN
        }
    }

    override fun toString(): String {
        return "${name}($id, $visibleName)"
    }
}
