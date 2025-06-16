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
    /**
     * An [IRegionFilter] for [VoiceRegion.SYDNEY], [VoiceRegion.INDIA], [VoiceRegion.JAPAN], [VoiceRegion.HONGKONG], [VoiceRegion.SINGAPORE], and [VoiceRegion.SOUTH_KOREA].
     */
    @JvmField
    val ASIA: IRegionFilter = object : IRegionFilter {
        val regions = listOf(VoiceRegion.SYDNEY, VoiceRegion.INDIA, VoiceRegion.JAPAN, VoiceRegion.HONGKONG,
            VoiceRegion.SINGAPORE, VoiceRegion.SOUTH_KOREA, VoiceRegion.ASIA, VoiceRegion.OCEANIA)

        override fun isRegionAllowed(node: LavalinkNode, region: VoiceRegion): RegionFilterVerdict {
            return if (region in regions) RegionFilterVerdict.PASS else RegionFilterVerdict.SOFT_BLOCK
        }
    }

    /**
     * An [IRegionFilter] for [VoiceRegion.ROTTERDAM], [VoiceRegion.RUSSIA], [VoiceRegion.AMSTERDAM], [VoiceRegion.MADRID], [VoiceRegion.MILAN],
     * [VoiceRegion.BUCHAREST], [VoiceRegion.EUROPE], [VoiceRegion.LONDON], [VoiceRegion.FINLAND], [VoiceRegion.FRANKFURT], and [VoiceRegion.STOCKHOLM].
     */
    @JvmField
    val EUROPE: IRegionFilter = object : IRegionFilter {
        val regions = listOf(VoiceRegion.ROTTERDAM, VoiceRegion.RUSSIA, VoiceRegion.AMSTERDAM, VoiceRegion.MADRID, VoiceRegion.MILAN,
            VoiceRegion.BUCHAREST, VoiceRegion.EUROPE, VoiceRegion.LONDON, VoiceRegion.FINLAND, VoiceRegion.FRANKFURT, VoiceRegion.STOCKHOLM,
            VoiceRegion.WARSAW, VoiceRegion.EUROPE)

        override fun isRegionAllowed(node: LavalinkNode, region: VoiceRegion): RegionFilterVerdict {
            return if (region in regions) RegionFilterVerdict.PASS else RegionFilterVerdict.SOFT_BLOCK
        }
    }

    /**
     * An [IRegionFilter] for [VoiceRegion.US_CENTRAL], [VoiceRegion.US_EAST], [VoiceRegion.US_WEST], [VoiceRegion.US_SOUTH], [VoiceRegion.ATLANTA],
     * [VoiceRegion.SEATTLE], [VoiceRegion.SANTA_CLARA], [VoiceRegion.NEWARK], [VoiceRegion.MONTREAL], [VoiceRegion.OREGON], and [VoiceRegion.ST_PETE].
     */
    @JvmField
    val US: IRegionFilter = object : IRegionFilter {
        val regions = listOf(VoiceRegion.US_CENTRAL, VoiceRegion.US_EAST, VoiceRegion.US_SOUTH, VoiceRegion.US_WEST, VoiceRegion.ATLANTA,
            VoiceRegion.SEATTLE, VoiceRegion.SANTA_CLARA, VoiceRegion.NEWARK, VoiceRegion.MONTREAL, VoiceRegion.OREGON, VoiceRegion.ST_PETE,
            VoiceRegion.CENTRAL_AMERICA)

        override fun isRegionAllowed(node: LavalinkNode, region: VoiceRegion): RegionFilterVerdict {
            return if (region in regions) RegionFilterVerdict.PASS else RegionFilterVerdict.SOFT_BLOCK
        }
    }

    /**
     * An [IRegionFilter] for [VoiceRegion.BRAZIL], [VoiceRegion.SANTIAGO], and [VoiceRegion.BUENOS_AIRES].
     */
    @JvmField
    val SOUTH_AMERICA: IRegionFilter = object : IRegionFilter {
        val regions = listOf(VoiceRegion.BRAZIL, VoiceRegion.SANTIAGO, VoiceRegion.BUENOS_AIRES, VoiceRegion.SOUTH_AMERICA)

        override fun isRegionAllowed(node: LavalinkNode, region: VoiceRegion): RegionFilterVerdict {
            return if (region in regions) RegionFilterVerdict.PASS else RegionFilterVerdict.SOFT_BLOCK
        }
    }

    /**
     * An [IRegionFilter] for [VoiceRegion.SOUTH_AFRICA].
     */
    @JvmField
    val AFRICA: IRegionFilter = object : IRegionFilter {
        val regions = listOf(VoiceRegion.SOUTH_AFRICA, VoiceRegion.AFRICA)

        override fun isRegionAllowed(node: LavalinkNode, region: VoiceRegion): RegionFilterVerdict {
            return if (region in regions) RegionFilterVerdict.PASS else RegionFilterVerdict.SOFT_BLOCK
        }
    }

    /**
     * An [IRegionFilter] for [VoiceRegion.TEL_AVIV] and [VoiceRegion.DUBAI].
     */
    @JvmField
    val MIDDLE_EAST: IRegionFilter = object : IRegionFilter {
        val regions = listOf(VoiceRegion.TEL_AVIV, VoiceRegion.DUBAI, VoiceRegion.MIDDLE_EAST)

        override fun isRegionAllowed(node: LavalinkNode, region: VoiceRegion): RegionFilterVerdict {
            return if (region in regions) RegionFilterVerdict.PASS else RegionFilterVerdict.SOFT_BLOCK
        }
    }


    /**
     * Gets a [RegionGroup] from a string. This method is case-insensitive.
     *
     * @param region The region to get the [RegionGroup] for.
     * Valid values are [AFRICA], [ASIA], [EUROPE], [MIDDLE_EAST], [SOUTH_AMERICA] and [US].
     */
    fun valueOf(region: String) = when (region.uppercase()) {
        "AFRICA" -> AFRICA
        "ASIA" -> ASIA
        "EUROPE" -> EUROPE
        "MIDDLE_EAST" -> MIDDLE_EAST
        "SOUTH_AMERICA" -> SOUTH_AMERICA
        "US" -> US
        else -> throw IllegalArgumentException("No region constant: $region")
    }
}

// TODO In case no exact server match, should it look for the closest node in that same region?
enum class VoiceRegion(
    val visibleName: String,
    val discId: String, // The discord voice server id
    val cfIds: List<String> // The new cf datacenter ids (IATA codes)
) {
    AMSTERDAM("Amsterdam", "amsterdam", listOf("ams")),
    ATLANTA("Atlanta", "atlanta", listOf("atl")),
    BRAZIL("Brazil", "brazil", listOf(
        "aru", "bel", "bnu", "bsb", "caw", "cfc", "cgb", "cnf", "cwb", "fln", "for",
        "gig", "gru", "gyn", "itj", "jdo", "joi", "mao", "nvt", "pmw", "poa", "qwj",
        "rao", "rec", "sjk", "sjp", "sod", "ssa", "udi", "vcp", "vix", "xap"
    )),
    BUCHAREST("Bucharest", "bucharest", listOf("otp")),
    BUENOS_AIRES("Buenos Aires", "buenos-aires", listOf(
        "eze", "cor", "nqn"
    )),
    DUBAI("Dubai", "dubai", listOf("dxb")),
    EUROPE("Europe", "europe", listOf(
        "tia","evn","vie","gyd", "llk","msq","bru","sof","zag","lca","prg","cph","bod",
        "cdg", "lys", "mrs","tbs","ath", "skg","bud","kef","dub", "ork","fco", "pmo","rix",
        "vno","lux","kiv", "skp", "osl", "lis", "beg", "bts", "bcn", "got", "gva", "zrh",
        "adb", "ist", "kbp", "edi", "man", "dus", "ham", "muc", "str", "txl")),
    FINLAND("Finland", "finland", listOf("hel")),
    FRANKFURT("Frankfurt", "frankfurt", listOf("fra")),
    HONGKONG("Hong Kong", "hongkong", listOf("hkg", "mfm")),
    INDIA("India", "india", listOf(
        "amd", "bbi", "blr", "bom", "ccu", "cnn", "cok", "del", "hyd", "ixc", "knu", "maa", "nag", "pat"
    )),
    JAPAN("Japan","japan", listOf("fuk", "kix", "nrt", "oka")),
    LONDON("London", "london", listOf("lhr")),
    MADRID("Madrid", "madrid", listOf("mad")),
    MILAN("Milan", "milan", listOf("mxp")),
    MONTREAL("Montreal", "montreal", listOf(
        "yul", "yhz", "yow", "yvr", "ywg", "yxe", "yyc", "yyz"
    )),
    NEWARK("Newark", "newark", listOf("ewr")),
    OREGON("Oregon", "oregon", listOf("pdx")),
    ROTTERDAM("Rotterdam","rotterdam", listOf("rtm")),
    RUSSIA("Russia", "russia", listOf("dme", "led", "kja", "svx")),
    SANTA_CLARA("Santa Clara", "santa-clara", listOf("sjc")),
    SANTIAGO("Santiago", "santiago", listOf("scl", "ari")),
    SEATTLE("Seattle", "seattle", listOf("sea")),
    SINGAPORE("Singapore", "singapore", listOf("sin")),
    SOUTH_AFRICA("South Africa","southafrica", listOf(
        "cpt", "dur", "jnb"
    )),
    SOUTH_KOREA("South Korea", "south-korea", listOf("icn")),
    ST_PETE("St Pete", "st-pete", listOf("pie")),
    STOCKHOLM("Stockholm", "stockholm", listOf("arn")),
    SYDNEY("Sydney", "sydney", listOf(
        "syd", "adl", "akl", "bne", "cbr", "chc", "hba", "mel", "per"
    )),
    TEL_AVIV("Tel Aviv", "tel-aviv", listOf("tlv", "hfa")),
    US_CENTRAL("US Central", "us-central", listOf(
        "cle", "cmh", "den", "fsd", "ind", "mci", "mem", "msp", "okc", "oma", "ord", "phx", "stl", "dtw"
    )),
    US_EAST("US East", "us-east", listOf(
        "bgr", "bos", "buf", "iad", "orf", "phl", "pit", "rdu", "ric"
    )),
    US_SOUTH("US South", "us-south", listOf(
        "aus", "bna", "clt", "dfw", "iah", "jax", "mfe", "mia", "sat", "tpa", "tlh"
    )),
    US_WEST("US West", "us-west", listOf(
        "abq", "anc", "hnl", "las", "lax", "san", "sfo", "slc", "smf"
    )),
    WARSAW("Warsaw", "warsaw", listOf("waw")),

    ASIA("Asia", "", listOf(
        "bhy", "can", "cgd", "cgo", "csx", "ctu", "czx", "dlc", "foc", "fuo", "hak",
        "hfe","hgh", "hyn", "jxg", "khn", "kmg", "kwe", "lhw", "nng", "pkx", "sha",
        "sjw","szx", "tao", "ten", "tna", "tsn", "tyn", "whu", "xfn", "xiy", "xnn",
        "zgn","khh", "tpe","uln","ceb", "cgy", "crk", "mnl","cgk", "dps", "jog","bkk",
        "cnx", "urt","dad", "han", "sgn","kch", "kul", "jhb", "bwn","pnh","vte","cgp",
        "dac", "jsr","cmb","isb", "khi", "lhe","ktm","pbh","mle","akx", "ala", "nqz","tas"
    )),
    CENTRAL_AMERICA("Central America", "", listOf(
        "gdl", "mex", "qro","gua","pty","sap", "tgu","sjo","bgi","gnd","kin","pos","sdq", "sti","sju",
    )),
    SOUTH_AMERICA("South America", "", listOf(
        "lim","lpb","gye", "uio","baq", "bog", "clo", "mde","geo","pbm"
    )),
    AFRICA("Africa", "", listOf(
        "aae", "alg", "orn", "tun","cai","abj", "ask","acc","dkr",
        "los","add","dar","ebb","kgl","mba","nbo","hre","jib",
        "mru","mpm", "oua","run","lun","fih","lad", "wdh","tnr"
    )),
    MIDDLE_EAST("Middle East", "", listOf(
        "bah","dmm", "ruh", "jed","doh", "kwi","mct","bey","amm",
        "bgw", "bsr", "ebl", "isu", "njf", "xnh","zdm"
    )),
    OCEANIA("Oceania", "", listOf(
        "nou","suv","gum","ppt"
    )),
    UNKNOWN("Unknown", "", listOf("local"));

    companion object {
        @JvmStatic
        fun fromEndpoint(endpoint: String): VoiceRegion {
            // Handle new cloudflare based servers (e.g c-ewr07-d2466d40.discord.media:xxxx)
            if (endpoint.startsWith("c-")) {
                return parseCFEndpoint(endpoint)
            }
            // Endpoints come in format like "seattle1865.discord.gg", trim to subdomain with no numbers
            val endpointRegex = "^([a-z\\-]+)[0-9]+.*:443\$".toRegex()
            val match = endpointRegex.find(endpoint) ?: return UNKNOWN
            val idFromEndpoint = match.groupValues[1]
            return entries.find { it.discId == idFromEndpoint } ?: UNKNOWN
        }

        @JvmStatic
        fun parseCFEndpoint(endpoint: String): VoiceRegion {
            val endpointRegex = "^[a-z-A-Z0-9]-([a-z]+)[0-9]+-[a-zA-Z0-9]+\\.discord\\.media:[0-9]+$".toRegex()
            val match = endpointRegex.find(endpoint) ?: return UNKNOWN
            val idFromEndpoint = match.groupValues[1]
            return entries.find { it.cfIds.contains(idFromEndpoint) } ?: UNKNOWN
        }
    }

    override fun toString(): String {
        return "${name}($visibleName, $discId, $cfIds)"
    }
}
