package dev.arbjerg.lavalink.client.loadbalancing

import dev.arbjerg.lavalink.client.LavalinkNode

interface ILoadBalancer {
    fun determineBestSocketForRegion(region: VoiceRegion): LavalinkNode
}
