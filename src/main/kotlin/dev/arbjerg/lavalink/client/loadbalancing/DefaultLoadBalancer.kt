package dev.arbjerg.lavalink.client.loadbalancing

import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.client.LavalinkNode

class DefaultLoadBalancer(private val client: LavalinkClient) : ILoadBalancer {

    // TODO: what happens if one of the nodes stops loading ANY tracks? It would gain preference because there's no load on it
    //  Keep track of stuck tracks?
    //  Keep track of load failures? (LoadResult.NoMatches)
    //  Keep track of track exceptions?
    override fun determineBestSocketForRegion(region: VoiceRegion) = determineBestNode0()

    private fun determineBestNode0(): LavalinkNode {
        val nodes = client.nodes

        // Don't bother calculating penalties if we only have one node.
        if (nodes.size == 1) {
            if (!nodes[0].available) {
                throw IllegalStateException("Node ${nodes[0].name} is unavailable!")
            }

            return nodes[0]
        }

        var leastPenalty: LavalinkNode? = null
        var record = Int.MAX_VALUE

        nodes.forEach { node ->
            val nodePenalties = node.penalties.calculateTotal()

            if (nodePenalties < record) {
                leastPenalty = node
                record = nodePenalties
            }
        }

        if (leastPenalty == null || !leastPenalty!!.available) {
            throw IllegalStateException("No available nodes!")
        }

        // Kotlin moment, I should probably properly learn kotlin and buy a book some day
        return leastPenalty!!
    }
}
