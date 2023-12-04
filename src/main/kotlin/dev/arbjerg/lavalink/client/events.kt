package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.protocol.Track
import dev.arbjerg.lavalink.protocol.v4.Message

internal fun Message.toClientEvent(node: LavalinkNode) = when (this) {
    is Message.ReadyEvent -> ReadyEvent(node, this)
    is Message.EmittedEvent.TrackEndEvent -> TrackEndEvent(node, this)
    is Message.EmittedEvent.TrackExceptionEvent -> TrackExceptionEvent(node, this)
    is Message.EmittedEvent.TrackStartEvent -> TrackStartEvent(node, this)
    is Message.EmittedEvent.TrackStuckEvent -> TrackStuckEvent(node, this)
    is Message.EmittedEvent.WebSocketClosedEvent -> WebSocketClosedEvent(node, this)
    is Message.PlayerUpdateEvent -> PlayerUpdateEvent(node, this)
    is Message.StatsEvent -> StatsEvent(node, this)
}

sealed class ClientEvent<T : Message>(open val node: LavalinkNode, open val event: T)

// Normal events
data class ReadyEvent(override val node: LavalinkNode, override val event: Message.ReadyEvent)
    : ClientEvent<Message.ReadyEvent>(node, event)

data class PlayerUpdateEvent(override val node: LavalinkNode, override val event: Message.PlayerUpdateEvent)
    : ClientEvent<Message.PlayerUpdateEvent>(node, event)

data class StatsEvent(override val node: LavalinkNode, override val event: Message.StatsEvent)
    : ClientEvent<Message.StatsEvent>(node, event)

// Player events
sealed class EmittedEvent<T : Message.EmittedEvent>(override val node: LavalinkNode, override val event: T)
    : ClientEvent<T>(node, event)

data class TrackStartEvent(override val node: LavalinkNode, override val event: Message.EmittedEvent.TrackStartEvent)
    : EmittedEvent<Message.EmittedEvent.TrackStartEvent>(node, event) {
        val track = Track(event.track)
    }

data class TrackEndEvent(override val node: LavalinkNode, override val event: Message.EmittedEvent.TrackEndEvent)
    : EmittedEvent<Message.EmittedEvent.TrackEndEvent>(node, event) {
    val track = Track(event.track)
}

data class TrackExceptionEvent(override val node: LavalinkNode, override val event: Message.EmittedEvent.TrackExceptionEvent)
    : EmittedEvent<Message.EmittedEvent.TrackExceptionEvent>(node, event) {
    val track = Track(event.track)
}

data class TrackStuckEvent(override val node: LavalinkNode, override val event: Message.EmittedEvent.TrackStuckEvent)
    : EmittedEvent<Message.EmittedEvent.TrackStuckEvent>(node, event) {
    val track = Track(event.track)
}

data class WebSocketClosedEvent(override val node: LavalinkNode, override val event: Message.EmittedEvent.WebSocketClosedEvent)
    : EmittedEvent<Message.EmittedEvent.WebSocketClosedEvent>(node, event)
