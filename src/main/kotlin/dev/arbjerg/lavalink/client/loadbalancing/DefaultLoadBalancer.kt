package dev.arbjerg.lavalink.client.loadbalancing

import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.client.LavalinkNode

class DefaultLoadBalancer(private val client: LavalinkClient) : ILoadBalancer {

    // TODO: what happens if one of the nodes stops loading ANY tracks? It would gain preference because there's no load on it
    //  Keep track of stuck tracks?
    //  Keep track of load failures? (LoadResult.NoMatches)
    //  Keep track of track exceptions?
    override fun determineBestSocketForRegion(region: VoiceRegion): LavalinkNode? {
        val nodes = client.nodes

        // TODO("Not yet implemented")

        return nodes.random()
    }
}
