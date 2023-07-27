package dev.arbjerg.lavalink.client.loadbalancing.builtin

import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.client.loadbalancing.ILoadBalancer
import dev.arbjerg.lavalink.client.loadbalancing.VoiceRegion

class DefaultLoadBalancer(private val client: LavalinkClient) : ILoadBalancer {
    private val penaltyProviders = mutableListOf<IPenaltyProvider>()

    // TODO: Why even do this LOL
    fun addPenaltyProvider(penaltyProvider: IPenaltyProvider) {
        penaltyProviders.add(penaltyProvider)
    }

    fun removePenaltyProvider(penaltyProvider: IPenaltyProvider) {
        penaltyProviders.remove(penaltyProvider)
    }

    // TODO: what happens if one of the nodes stops loading ANY tracks? It would gain preference because there's no load on it
    //  Keep track of stuck tracks?
    //  Keep track of load failures? (LoadResult.NoMatches)
    //  Keep track of track exceptions?
    override fun determineBestSocketForRegion(region: VoiceRegion): LavalinkNode {
        val nodes = client.nodes

        // Don't bother calculating penalties if we only have one node.
        if (nodes.size == 1) {
            val node = nodes.first()
            if (!node.available) {
                throw IllegalStateException("Node ${nodes[0].name} is unavailable!")
            }

            return node
        }

        return nodes.filter { it.available }.minByOrNull { node ->
            node.penalties.calculateTotal() + penaltyProviders.sumOf { it.getPenalty(node, region) }
        } ?: throw IllegalStateException("No available nodes!")
    }
}
