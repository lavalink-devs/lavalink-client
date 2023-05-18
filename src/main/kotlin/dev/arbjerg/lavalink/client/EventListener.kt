package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.protocol.v4.Message

fun interface EventListener {
    fun onEvent(event: Message.EmittedEvent)
}
