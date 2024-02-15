package dev.arbjerg.lavalink.client.loadbalancing

import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.client.loadbalancing.builtin.IPenaltyProvider

interface ILoadBalancer {
    /**
     * Selects a node based on the criteria of the load balancer.
     *
     * @return The best node that matches the criteria
     * @throws RuntimeException when no nodes are available
     *
     * @see #selectNode(VoiceRegion)
     */
    fun selectNode(): LavalinkNode {
        return selectNode(null, null)
    }

    /**
     * Selects a node based on the criteria of the load balancer.
     * @param region A voice region may be provided to filter on the closest region to this node
     * @param guildId The ID of the guild to be associated with the returned node
     *
     * @return The best node that matches the criteria
     * @throws RuntimeException when no nodes are available
     */
    fun selectNode(region: VoiceRegion?, guildId: Long?): LavalinkNode

    /**
     * Adds a penalty provider to the load balancer.
     */
    fun addPenaltyProvider(penaltyProvider: IPenaltyProvider)

    /**
     * Removes a penalty provider from the load balancer.
     */
    fun removePenaltyProvider(penaltyProvider: IPenaltyProvider)
}
