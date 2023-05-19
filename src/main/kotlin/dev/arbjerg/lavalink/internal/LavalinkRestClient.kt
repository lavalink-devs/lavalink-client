package dev.arbjerg.lavalink.internal

import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.arbjerg.lavalink.protocol.v4.Player
import dev.arbjerg.lavalink.protocol.v4.PlayerUpdate
import dev.arbjerg.lavalink.protocol.v4.Players
import reactor.core.publisher.Mono

class LavalinkRestClient(val node: LavalinkNode) {
    fun getPlayers(): Mono<Players> {
        // GET /v4/sessions/{sessionId}/players
        TODO("Not yet implemented")
    }

    // TODO: where to store session id?
    fun getPlayer(guildId: Long): Mono<Player?> {
        // GET /v4/sessions/{sessionId}/players/{guildId}
        // Keep track of players locally and create one if needed?
        TODO("Not yet implemented")
    }

    fun updatePlayer(player: PlayerUpdate, guildId: ULong, noReplace: Boolean = true): Mono<Player> {
        // PATCH /v4/sessions/{sessionId}/players/{guildId}?noReplace=true
        TODO("Not yet implemented")
    }

    fun loadItem(identifier: String): Mono<LoadResult> {
        TODO("Not yet implemented")
    }
}
