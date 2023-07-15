@file:JvmName("Discord4JUtils")

package dev.arbjerg.lavalink.libraries.discord4j

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient
import discord4j.discordjson.json.gateway.VoiceStateUpdate
import discord4j.gateway.json.ShardGatewayPayload
import reactor.core.publisher.Mono

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
): Mono<Void> {
    val update = VoiceStateUpdate.builder()
        .guildId(guildId.asLong())
        .selfDeaf(selfDeaf)
        .selfMute(selfMute)
        .build()
    val shardId = gatewayClientGroup.computeShardIndex(guildId)
    val payload = ShardGatewayPayload.voiceStateUpdate(update, shardId)
    return gatewayClientGroup.unicast(payload)
}
