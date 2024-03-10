package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.protocol.v4.*
import dev.arbjerg.lavalink.client.protocol.Track
import dev.arbjerg.lavalink.client.protocol.TrackException
import dev.arbjerg.lavalink.client.protocol.toCustom
import dev.arbjerg.lavalink.protocol.v4.Message.EmittedEvent.TrackEndEvent.AudioTrackEndReason

internal fun Message.toClientEvent(node: LavalinkNode) = when (this) {
    is Message.ReadyEvent -> ReadyEvent(node, resumed, sessionId)
    is Message.EmittedEvent.TrackEndEvent -> TrackEndEvent(node, guildId.toLong(), track.toCustom(), reason)
    is Message.EmittedEvent.TrackExceptionEvent -> TrackExceptionEvent(node, guildId.toLong(), track.toCustom(), exception.toCustom())
    is Message.EmittedEvent.TrackStartEvent -> TrackStartEvent(node, guildId.toLong(), track.toCustom())
    is Message.EmittedEvent.TrackStuckEvent -> TrackStuckEvent(node, guildId.toLong(), track.toCustom(), thresholdMs)
    is Message.EmittedEvent.WebSocketClosedEvent -> WebSocketClosedEvent(node, guildId.toLong(), code, reason, byRemote)
    is Message.PlayerUpdateEvent -> PlayerUpdateEvent(node, guildId.toLong(), state)
    is Message.StatsEvent -> StatsEvent(node, frameStats, players, playingPlayers, uptime, memory, cpu)
}

sealed class ClientEvent(open val node: LavalinkNode)

// Normal events
data class ReadyEvent(override val node: LavalinkNode, val resumed: Boolean, val sessionId: String)
    : ClientEvent(node)

data class PlayerUpdateEvent(override val node: LavalinkNode, val guildId: Long, val state: PlayerState)
    : ClientEvent(node)

data class StatsEvent(
    override val node: LavalinkNode,
    val frameStats: FrameStats?,
    val players: Int,
    val playingPlayers: Int,
    val uptime: Long,
    val memory: Memory,
    val cpu: Cpu
) : ClientEvent(node)

// Player events
sealed class EmittedEvent(override val node: LavalinkNode, open val guildId: Long)
    : ClientEvent(node)

data class TrackStartEvent(override val node: LavalinkNode, override val guildId: Long, val track: Track)
    : EmittedEvent(node, guildId)

data class TrackEndEvent(override val node: LavalinkNode, override val guildId: Long, val track: Track, val endReason: AudioTrackEndReason)
    : EmittedEvent(node, guildId)

data class TrackExceptionEvent(override val node: LavalinkNode, override val guildId: Long, val track: Track, val exception: TrackException)
    : EmittedEvent(node, guildId)

data class TrackStuckEvent(override val node: LavalinkNode, override val guildId: Long, val track: Track, val thresholdMs: Long)
    : EmittedEvent(node, guildId)

data class WebSocketClosedEvent(override val node: LavalinkNode, override val guildId: Long, val code: Int, val reason: String, val byRemote: Boolean)
    : EmittedEvent(node, guildId)
