package dev.arbjerg.lavalink.internal

import dev.arbjerg.lavalink.client.LavalinkPlayer
import dev.arbjerg.lavalink.protocol.v4.Player

internal fun Player.toLavalinkPlayer(rest: LavalinkRestClient) = LavalinkPlayer(rest, this)
//fun String.toUnsignedLong() = java.lang.Long.parseUnsignedLong(this)
