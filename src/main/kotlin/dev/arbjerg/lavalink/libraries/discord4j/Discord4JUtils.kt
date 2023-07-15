@file:JvmName("Discord4JUtils")

package dev.arbjerg.lavalink.libraries.discord4j

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.discordjson.Id
import discord4j.discordjson.json.gateway.VoiceStateUpdate
import discord4j.gateway.json.GatewayPayload
import discord4j.voice.VoiceConnection
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*

/**
 * Joins [channel] without creating a [VoiceConnection].
 *
 * **Important:** If you want to use want to use this with Lavalink call [installVoiceHandler] first.
 *
 * Java users use:
 * ```java
 * Discord4JUtils.joinChannel(gatewayClient, voiceChannel);
 * ```
 */
@JvmOverloads
fun GatewayDiscordClient.joinChannel(
    channel: VoiceChannel,
    selfMute: Boolean = false,
    selfDeaf: Boolean = false
) =
    sendVoiceUpdate(this, channel.guildId, channel.id, selfMute, selfDeaf)

/**
 * Disconnects from the channel on [guildId].
 *
 * **Important:** If you want to use want to use this with Lavalink call [installVoiceHandler] first.
 *
 * Java users use:
 * ```java
 * Discord4JUtils.leave(gatewayClient, guildId);
 * ```
 */
@JvmOverloads
fun GatewayDiscordClient.leave(
    guildId: Snowflake,
    selfMute: Boolean = false,
    selfDeaf: Boolean = false
) =
    sendVoiceUpdate(this, guildId, channelId = null, selfMute, selfDeaf)

private fun sendVoiceUpdate(
    discordClient: GatewayDiscordClient,
    guildId: Snowflake,
    channelId: Snowflake?,
    selfMute: Boolean = false,
    selfDeaf: Boolean = false,
): Mono<Void> {
    val update = VoiceStateUpdate.builder()
        .guildId(guildId.asLong())
        .channelId(Optional.ofNullable(channelId).map { Id.of(it.asLong()) })
        .selfDeaf(selfDeaf)
        .selfMute(selfMute)
        .build()
    val payload = GatewayPayload.voiceStateUpdate(update)
    return discordClient.getGatewayClient(discordClient.getShardIdForGuild(guildId.asLong()))
        .get()
        .send(payload.toMono())
}

private fun GatewayDiscordClient.getShardIdForGuild(snowflake: Long): Int =
    ((snowflake shr 22) % gatewayClientGroup.shardCount).toInt()
