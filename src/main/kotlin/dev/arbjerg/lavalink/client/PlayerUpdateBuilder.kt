package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.internal.toLavalinkPlayer
import dev.arbjerg.lavalink.protocol.v4.*
import reactor.core.publisher.Mono
import kotlin.math.max
import kotlin.math.min

class PlayerUpdateBuilder internal constructor(private val node: LavalinkNode, private val guildId: Long) : IUpdatablePlayer {
    private var encodedTrack: Omissible<String?> = Omissible.omitted()
    private var identifier: Omissible<String> = Omissible.omitted()
    private var position: Omissible<Long> = Omissible.omitted()
    private var endTime: Omissible<Long?> = Omissible.omitted()
    private var volume: Omissible<Int> = Omissible.omitted()
    private var paused: Omissible<Boolean> = Omissible.omitted()
    private var filters: Omissible<Filters> = Omissible.omitted()
    private var state: Omissible<VoiceState> = Omissible.omitted()
    private var noReplace = false

    override fun setEncodedTrack(encodedTrack: String?): PlayerUpdateBuilder {
        this.encodedTrack = Omissible.of(encodedTrack)
        return this
    }

    override fun clearEncodedTrack(): PlayerUpdateBuilder {
        this.encodedTrack = Omissible.omitted()
        return this
    }

    override fun setIdentifier(identifier: String?): PlayerUpdateBuilder {
        this.identifier = identifier.toOmissible()
        return this
    }

    override fun setPosition(position: Long?): PlayerUpdateBuilder {
        this.position = position.toOmissible()
        return this
    }

    override fun setEndTime(endTime: Long?): PlayerUpdateBuilder {
        this.endTime = Omissible.of(endTime)
        return this
    }

    override fun clearEndTime(): PlayerUpdateBuilder {
        this.endTime = Omissible.omitted()
        return this
    }

    override fun setVolume(volume: Int): PlayerUpdateBuilder {
        this.volume = min(1000, max(0, volume)).toOmissible()
        return this
    }

    override fun setPaused(paused: Boolean): PlayerUpdateBuilder {
        this.paused = paused.toOmissible()
        return this
    }

    override fun setFilters(filters: Filters): PlayerUpdateBuilder {
        this.filters = filters.toOmissible()
        return this
    }

    override fun setVoiceState(state: VoiceState): PlayerUpdateBuilder {
        this.state = state.toOmissible()
        return this
    }

    fun setNoReplace(noReplace: Boolean): PlayerUpdateBuilder {
        this.noReplace = noReplace
        return this
    }

    fun build() = PlayerUpdate(
        encodedTrack, identifier, position, endTime, volume, paused, filters, state
    )

    fun asMono(): Mono<LavalinkPlayer> {
        /*val cachedPlayer = node.playerCache[guildId]

        if ((cachedPlayer == null || !cachedPlayer.state.connected) && state is Omissible.Omitted) {
            return Mono.error(IllegalStateException("Player is not connected to a voice channel."))
        }*/

        return node.rest.updatePlayer(build(), guildId, noReplace)
            .map { it.toLavalinkPlayer(node) }
            .doOnNext {
                // Update player in cache
                node.playerCache[guildId] = it
            }
    }
}
