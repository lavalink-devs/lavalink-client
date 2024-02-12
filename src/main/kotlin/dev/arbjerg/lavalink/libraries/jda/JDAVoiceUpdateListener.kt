package dev.arbjerg.lavalink.libraries.jda

import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.client.LinkState
import dev.arbjerg.lavalink.client.loadbalancing.VoiceRegion
import dev.arbjerg.lavalink.protocol.v4.VoiceState
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor

class JDAVoiceUpdateListener(private val lavalink: LavalinkClient) : VoiceDispatchInterceptor {
    override fun onVoiceServerUpdate(update: VoiceDispatchInterceptor.VoiceServerUpdate) {
        val state = VoiceState(
            update.token,
            update.endpoint,
            update.sessionId
        )
        val region = VoiceRegion.fromEndpoint(update.endpoint)
        val link = lavalink.getOrCreateLink(update.guildIdLong, region)

        link.onVoiceServerUpdate(state)
    }

    override fun onVoiceStateUpdate(update: VoiceDispatchInterceptor.VoiceStateUpdate): Boolean {
        val channel = update.channel
        val link = lavalink.getLinkIfCached(update.guildIdLong) ?: return false
        val player = link.node.getCachedPlayer(update.guildIdLong) ?: return false
        val playerState = player.state

        if (channel == null) {
            if (playerState.connected) {
                link.state = LinkState.CONNECTED
            } else {
                link.state = LinkState.DISCONNECTED
                link.destroy().subscribe()
            }
        }

        // We return true if a connection was previously established.
        return playerState.connected
    }
}
