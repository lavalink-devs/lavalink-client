package dev.arbjerg.lavalink.client

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
data class EmittedEvent(override val node: LavalinkNode, override val event: Message.EmittedEvent)
    : ClientEvent<Message.EmittedEvent>(node, event)

data class TrackStartEvent(override val node: LavalinkNode, override val event: Message.EmittedEvent.TrackStartEvent)
    : ClientEvent<Message.EmittedEvent.TrackStartEvent>(node, event)

data class TrackEndEvent(override val node: LavalinkNode, override val event: Message.EmittedEvent.TrackEndEvent)
    : ClientEvent<Message.EmittedEvent.TrackEndEvent>(node, event)

data class TrackExceptionEvent(override val node: LavalinkNode, override val event: Message.EmittedEvent.TrackExceptionEvent)
    : ClientEvent<Message.EmittedEvent.TrackExceptionEvent>(node, event)

data class TrackStuckEvent(override val node: LavalinkNode, override val event: Message.EmittedEvent.TrackStuckEvent)
    : ClientEvent<Message.EmittedEvent.TrackStuckEvent>(node, event)

data class WebSocketClosedEvent(override val node: LavalinkNode, override val event: Message.EmittedEvent.WebSocketClosedEvent)
    : ClientEvent<Message.EmittedEvent.WebSocketClosedEvent>(node, event)
