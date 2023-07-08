package dev.arbjerg.lavalink.client.loadbalancing

import dev.arbjerg.lavalink.client.LavalinkNode

fun interface ILoadBalancer {
    fun determineBestSocketForRegion(region: VoiceRegion): LavalinkNode
}
