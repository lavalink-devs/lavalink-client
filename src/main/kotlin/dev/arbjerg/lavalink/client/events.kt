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

sealed class ClientEvent<T : Message>(open val node: LavalinkNode)

// Normal events
data class ReadyEvent(override val node: LavalinkNode, val resumed: Boolean, val sessionId: String)
    : ClientEvent<Message.ReadyEvent>(node)

data class PlayerUpdateEvent(override val node: LavalinkNode, val guildId: Long, val state: PlayerState)
    : ClientEvent<Message.PlayerUpdateEvent>(node)

data class StatsEvent(
    override val node: LavalinkNode,
    val frameStats: FrameStats?,
    val players: Int,
    val playingPlayers: Int,
    val uptime: Long,
    val memory: Memory,
    val cpu: Cpu
) : ClientEvent<Message.StatsEvent>(node)

// Player events
sealed class EmittedEvent<T : Message.EmittedEvent>(override val node: LavalinkNode)
    : ClientEvent<T>(node)

data class TrackStartEvent(override val node: LavalinkNode, val guildId: Long, val track: Track)
    : EmittedEvent<Message.EmittedEvent.TrackStartEvent>(node)

data class TrackEndEvent(override val node: LavalinkNode, val guildId: Long, val track: Track, val endReason: AudioTrackEndReason)
    : EmittedEvent<Message.EmittedEvent.TrackEndEvent>(node)

data class TrackExceptionEvent(override val node: LavalinkNode, val guildId: Long, val track: Track, val exception: TrackException)
    : EmittedEvent<Message.EmittedEvent.TrackExceptionEvent>(node)

data class TrackStuckEvent(override val node: LavalinkNode, val guildId: Long, val track: Track, val thresholdMs: Long)
    : EmittedEvent<Message.EmittedEvent.TrackStuckEvent>(node)

data class WebSocketClosedEvent(override val node: LavalinkNode, val guildId: Long, val code: Int, val reason: String, val byRemote: Boolean)
    : EmittedEvent<Message.EmittedEvent.WebSocketClosedEvent>(node)
