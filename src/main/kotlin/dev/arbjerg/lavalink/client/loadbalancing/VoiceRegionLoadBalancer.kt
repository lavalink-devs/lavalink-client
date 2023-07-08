package dev.arbjerg.lavalink.client.loadbalancing

import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.client.LavalinkNode

class VoiceRegionLoadBalancer(private val client: LavalinkClient) : ILoadBalancer {
    override fun determineBestSocketForRegion(region: VoiceRegion): LavalinkNode {
        TODO("Not yet implemented")
    }
}
