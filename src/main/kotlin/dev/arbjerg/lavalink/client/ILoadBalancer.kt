package dev.arbjerg.lavalink.client

interface ILoadBalancer {
    fun determineBestNode(): LavalinkNode
}
