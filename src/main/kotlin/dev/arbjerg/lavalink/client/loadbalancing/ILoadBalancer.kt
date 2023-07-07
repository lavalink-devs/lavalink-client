package dev.arbjerg.lavalink.client.loadbalancing

import dev.arbjerg.lavalink.client.LavalinkNode

interface ILoadBalancer {
    fun determineBestNode(): LavalinkNode? = determineBestSocketForRegion(VoiceRegion.NONE)
    fun determineBestSocketForRegion(region: VoiceRegion): LavalinkNode?
}
