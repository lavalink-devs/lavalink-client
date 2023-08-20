package dev.arbjerg.lavalink.internal

import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.client.LavalinkPlayer
import dev.arbjerg.lavalink.protocol.v4.Player

internal fun Player.toLavalinkPlayer(node: LavalinkNode) = LavalinkPlayer(node, this)
//fun String.toUnsignedLong() = java.lang.Long.parseUnsignedLong(this)
