package dev.arbjerg.lavalink.client.loadbalancing.builtin

import dev.arbjerg.lavalink.client.loadbalancing.MAX_ERROR
import dev.arbjerg.lavalink.protocol.v4.Message

interface INodeHealthProvider {
    /**
     * Called for each event on the node.
     */
    fun handleTrackEvent(event: Message.EmittedEvent)

    /**
     * Calculate the penalty for the node based off of its health.
     *
     * Return value should never exceed [MAX_ERROR]. Lower means to take preference.
     *
     * @return A number between 0 and [MAX_ERROR] (inclusive), using numbers outside of this range may cause errors.
     */
    fun calculateTotalHealthPenalty(): Int

    /**
     * Gives a simple answer if the node is considered healthy.
     *
     * @return true if the node is in a healthy state
     */
    fun isHealthy() : Boolean
}
