@file:JvmName("D4JVoiceHandler")

package dev.arbjerg.lavalink.libraries.discord4j

import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.client.LinkState
import dev.arbjerg.lavalink.client.loadbalancing.VoiceRegion
import dev.arbjerg.lavalink.protocol.v4.VoiceState
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.VoiceServerUpdateEvent
import discord4j.core.event.domain.VoiceStateUpdateEvent
import reactor.core.Disposable
import reactor.core.Disposables
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
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
    // guild_id -> channel_id
    val channelIdCache = ConcurrentHashMap<Long, String>()

    val voiceStateUpdate = on(VoiceStateUpdateEvent::class.java) { event ->
        val update = event.current
        if (update.userId != update.client.selfId) return@on Mono.empty()
        val channel = update.channelId.getOrNull()
        val guildId = update.guildId.asLong()

        channel?.let { channelSnowflake ->
            channelIdCache[guildId] = channelSnowflake.asString()
        }

        val link = lavalink.getLinkIfCached(guildId) ?: return@on Mono.empty()
        val player = link.node.playerCache[guildId] ?: return@on Mono.empty()
        val playerState = player.state

        if (channel == null && playerState.connected) {
            link.state = LinkState.DISCONNECTED
            link.destroy()
        } else {
            link.state = LinkState.CONNECTED
            Mono.empty()
        }
    }.subscribe()

    val voiceServerUpdate = on(VoiceServerUpdateEvent::class.java) { update ->
        val guildId = update.guildId.asLong()
        val state = VoiceState(
            update.token,
            update.endpoint!!,
            getGatewayClient(update.shardInfo.index).get().sessionId,
            channelIdCache.remove(guildId) // TODO: Test if this works, is the order of events correct?
        )

        val region = VoiceRegion.fromEndpoint(update.endpoint!!)
        val link = lavalink.getOrCreateLink(guildId, region)

        link.onVoiceServerUpdate(state)
        Mono.empty<Unit>()
    }.subscribe()

    return Disposables.composite(voiceStateUpdate, voiceServerUpdate)
}
