package dev.arbjerg.lavalink.libraries.jda

import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.protocol.v4.VoiceState
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor

class JDAVoiceUpdateListener(private val lavalink: LavalinkClient) : VoiceDispatchInterceptor {
    override fun onVoiceServerUpdate(update: VoiceDispatchInterceptor.VoiceServerUpdate) {
        val state = VoiceState(
            update.token,
            update.endpoint,
            update.sessionId
        )

        val node = lavalink.getLink(update.guildIdLong).node

        node.getPlayer(update.guildIdLong)
            .subscribe {
                it.setVoiceState(state).asMono().block()
            }
    }

    override fun onVoiceStateUpdate(update: VoiceDispatchInterceptor.VoiceStateUpdate): Boolean {
        val channel = update.channel
        val link = lavalink.getLink(update.guildIdLong)
        val player = link.getPlayer().block()!!
        val playerState = player.state

        if (channel == null) {
            if (playerState.connected) {
                link.node.destroyPlayer(update.guildIdLong).block()
            }
        }

        return playerState.connected
    }
}
