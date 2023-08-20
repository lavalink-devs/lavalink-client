package dev.arbjerg.lavalink.libraries.jda

import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.client.PlayerUpdateBuilder
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

        val region = VoiceRegion(update.endpoint, update.endpoint)
        val node = lavalink.getLink(update.guildIdLong, region).node

        PlayerUpdateBuilder(node, update.guildIdLong)
            .setVoiceState(state)
            .asMono()
            .block()
    }

    override fun onVoiceStateUpdate(update: VoiceDispatchInterceptor.VoiceStateUpdate): Boolean {
        val channel = update.channel
        val link = lavalink.getLink(update.guildIdLong)
        val player = link.node.playerCache[update.guildIdLong] ?: return false
        val playerState = player.state

        if (channel == null) {
            if (!playerState.connected) {
                link.destroyPlayer().block()
            }
        }

        return playerState.connected
    }
}
