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
        val regions = listOf(VoiceRegion.SYDNEY, VoiceRegion.INDIA, VoiceRegion.JAPAN, VoiceRegion.HONGKONG, VoiceRegion.SINGAPORE)

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
        val regions = listOf(VoiceRegion.US_CENTRAL, VoiceRegion.US_EAST, VoiceRegion.US_SOUTH, VoiceRegion.US_WEST)

        override fun isRegionAllowed(node: LavalinkNode, region: VoiceRegion): RegionFilterVerdict {
            return if (region in regions) RegionFilterVerdict.PASS else RegionFilterVerdict.SOFT_BLOCK
        }
    }
    @JvmField
    val SOUTH_AMERICA: IRegionFilter = object : IRegionFilter {
        val regions = listOf(VoiceRegion.BRAZIL)

        override fun isRegionAllowed(node: LavalinkNode, region: VoiceRegion): RegionFilterVerdict {
            return if (region in regions) RegionFilterVerdict.PASS else RegionFilterVerdict.SOFT_BLOCK
        }
    }
    @JvmField
    val AFRICA: IRegionFilter = object : IRegionFilter {
        val regions = listOf(VoiceRegion.SOUTH_AFRICA)

        override fun isRegionAllowed(node: LavalinkNode, region: VoiceRegion): RegionFilterVerdict {
            return if (region in regions) RegionFilterVerdict.PASS else RegionFilterVerdict.SOFT_BLOCK
        }
    }


    /**
     * Gets a [RegionGroup] from a string. This method is case-insensitive.
     *
     * @param region The region to get the [RegionGroup] for. Valid values are [ASIA], [EUROPE], and [US].
     */
    fun valueOf(region: String) = when (region.uppercase()) {
        "ASIA" -> ASIA
        "EUROPE" -> EUROPE
        "US" -> US
        "SOUTH_AMERICA" -> SOUTH_AMERICA
        "AFRICA" -> AFRICA
        else -> throw IllegalArgumentException("No region constant: $region")
    }
}

// TODO In case no exact server match, should it look for the closest node in that same region?
enum class VoiceRegion(val id: String, val subregions: HashSet<String>, val visibleName: String) {
    BRAZIL("brazil", hashSetOf("buenos-aires", "santiago"), "Brazil"),
    HONGKONG("hongkong", hashSetOf(), "Hong Kong"),
    INDIA("india", hashSetOf(), "India"),
    JAPAN("japan", hashSetOf("south-korea"),"Japan"),
    ROTTERDAM("rotterdam", hashSetOf("frankfurt", "stockholm", "bucharest", "milan", "madrid"),"Rotterdam"),
    RUSSIA("russia", hashSetOf(),"Russia"),
    SINGAPORE("singapore", hashSetOf(),"Singapore"),
    SOUTH_AFRICA("southafrica", hashSetOf("dubai", "tel-aviv"),"South Africa"),
    SYDNEY("sydney", hashSetOf(), "Sydney"),
    US_CENTRAL("us-central", hashSetOf(), "US Central"),
    US_EAST("us-east", hashSetOf("newark"), "US East"),
    US_SOUTH("us-south", hashSetOf("atlanta"), "US South"),
    US_WEST("us-west", hashSetOf("seattle", "santa-clara"), "US West"),

    UNKNOWN("", hashSetOf(), "Unknown");

    companion object {
        @JvmStatic
        fun fromEndpoint(endpoint: String): VoiceRegion {
            // Endpoints come in format like "seattle1865.discord.gg", trim to subdomain with no numbers
            val trimmedEndpoint =  endpoint.split(".")[0].filter { !it.isDigit() }

            for(region in VoiceRegion.values()) {
                if (trimmedEndpoint == region.id || region.subregions.contains(trimmedEndpoint)) {
                    return region
                }
            }
            return UNKNOWN
        }
    }

    override fun toString(): String {
        return "${name}($id, $visibleName, subregions: ${subregions.toArray()})"
    }
}
