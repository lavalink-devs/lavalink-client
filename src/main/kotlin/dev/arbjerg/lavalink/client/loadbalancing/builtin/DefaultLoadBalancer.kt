package dev.arbjerg.lavalink.client.loadbalancing.builtin

import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.client.loadbalancing.ILoadBalancer
import dev.arbjerg.lavalink.client.loadbalancing.VoiceRegion

// TODO: have a look at this https://medium.com/javarevisited/load-balancing-algorithms-that-can-be-used-in-java-applications-6f605d1bf19
class DefaultLoadBalancer(private val client: LavalinkClient) : ILoadBalancer {
    private val penaltyProviders = mutableListOf<IPenaltyProvider>()

    override fun addPenaltyProvider(penaltyProvider: IPenaltyProvider) {
        penaltyProviders.add(penaltyProvider)
    }

    override fun removePenaltyProvider(penaltyProvider: IPenaltyProvider) {
        penaltyProviders.remove(penaltyProvider)
    }

    override fun selectNode(region: VoiceRegion?, guildId: Long?): LavalinkNode {
        val nodes = client.nodes

        // Don't bother calculating penalties if we only have one node.
        if (nodes.size == 1) {
            val node = nodes.first()

            if (!node.available) {
                throw IllegalStateException("Node ${nodes[0].name} is unavailable!")
            }

            return node
        }

        // TODO: Probably should enforce that no nodes go above the max
        return nodes.filter { it.available }.minByOrNull { node ->
            node.penalties.calculateTotal() + penaltyProviders.sumOf { it.getPenalty(node, region) }
        } ?: throw IllegalStateException("No available nodes!")
    }
}
