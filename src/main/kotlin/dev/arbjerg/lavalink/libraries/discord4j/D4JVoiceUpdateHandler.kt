@file:JvmName("D4JVoiceHandler")

package dev.arbjerg.lavalink.libraries.discord4j

import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.client.loadbalancing.VoiceRegion
import dev.arbjerg.lavalink.protocol.v4.VoiceState
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.VoiceServerUpdateEvent
import discord4j.core.event.domain.VoiceStateUpdateEvent
import reactor.core.Disposable
import reactor.core.Disposables
import reactor.core.publisher.Mono
import kotlin.jvm.optionals.getOrNull

/**
 * Install the Lavalink voice handler to this client.
 *
 * Java users use:
 * ```java
 * D4JVoiceHandler.install(gatewayClient, lavalink);
 * ```
 */
@JvmName("install")
fun GatewayDiscordClient.installVoiceHandler(lavalink: LavalinkClient): Disposable.Composite {
    val voiceStateUpdate = on(VoiceStateUpdateEvent::class.java) { event ->
        val update = event.current
        if (update.userId != update.client.selfId) return@on Mono.empty()
        val channel = update.channelId.getOrNull()
        val link = lavalink.getLink(update.guildId.asLong())
        val player = link.node.playerCache[update.guildId.asLong()] ?: return@on Mono.empty()
        val playerState = player.state

        if (channel == null && playerState.connected) {
            link.destroyPlayer()
        } else {
            Mono.empty()
        }
    }.subscribe()

    val voiceServerUpdate = on(VoiceServerUpdateEvent::class.java) { update ->
        val state = VoiceState(
            update.token,
            update.endpoint!!,
            getGatewayClient(update.shardInfo.index).get().sessionId
        )

        val region = VoiceRegion(update.endpoint!!, update.endpoint!!)
        val node = lavalink.getLink(update.guildId.asLong(), region).node

        node.createOrUpdatePlayer(update.guildId.asLong())
            .setVoiceState(state)
            .asMono()
    }.subscribe()

    return Disposables.composite(voiceStateUpdate, voiceServerUpdate)
}
