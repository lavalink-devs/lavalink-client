package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.internal.LavalinkRestClient
import dev.arbjerg.lavalink.internal.toLavalinkPlayer
import dev.arbjerg.lavalink.protocol.v4.*
import reactor.core.publisher.Mono
import kotlin.math.max
import kotlin.math.min

// TODO: where to put "noReplace"
class PlayerUpdateBuilder(private val rest: LavalinkRestClient, private val guildId: ULong) {
    private var encodedTrack: Omissible<String?> = Omissible.omitted()
    private var identifier: Omissible<String> = Omissible.omitted()
    private var position: Omissible<Long> = Omissible.omitted()
    private var endTime: Omissible<Long?> = Omissible.omitted()
    private var volume: Omissible<Int> = Omissible.omitted()
    private var paused: Omissible<Boolean> = Omissible.omitted()
    private var filters: Omissible<Filters> = Omissible.omitted()
    private var state: Omissible<VoiceState> = Omissible.omitted()
    private var noReplace = false

    fun setEncodedTrack(encodedTrack: String?): PlayerUpdateBuilder {
        this.encodedTrack = encodedTrack.toOmissible()
        return this
    }

    fun setIdentifier(identifier: String?): PlayerUpdateBuilder {
        this.identifier = identifier.toOmissible()
        return this
    }

    fun setPosition(position: Long?): PlayerUpdateBuilder {
        this.position = position.toOmissible()
        return this
    }

    fun setEndTime(endTime: Long?): PlayerUpdateBuilder {
        this.endTime = endTime.toOmissible()
        return this
    }

    fun setVolume(volume: Int): PlayerUpdateBuilder {
        this.volume = min(1000, max(0, volume)).toOmissible()
        return this
    }

    fun setPaused(paused: Boolean): PlayerUpdateBuilder {
        this.paused = paused.toOmissible()
        return this
    }

    fun setFilters(filters: Filters): PlayerUpdateBuilder {
        this.filters = filters.toOmissible()
        return this
    }

    fun setVoiceState(state: VoiceState): PlayerUpdateBuilder {
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
        return rest.updatePlayer(build(), guildId, noReplace)
            .map { it.toLavalinkPlayer(rest) }
    }
}
