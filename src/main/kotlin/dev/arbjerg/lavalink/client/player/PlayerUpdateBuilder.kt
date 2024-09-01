package dev.arbjerg.lavalink.client.player

import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.client.exception.VoiceStateException
import dev.arbjerg.lavalink.internal.toKotlin
import dev.arbjerg.lavalink.internal.toLavalinkPlayer
import dev.arbjerg.lavalink.protocol.v4.*
import reactor.core.CoreSubscriber
import reactor.core.publisher.Mono

class PlayerUpdateBuilder internal constructor(private val node: LavalinkNode, private val guildId: Long) : Mono<LavalinkPlayer>(),
    IUpdatablePlayer {
    private var trackUpdate: Omissible<PlayerUpdateTrack> = Omissible.omitted()
    private var position: Omissible<Long> = Omissible.omitted()
    private var endTime: Omissible<Long?> = Omissible.omitted()
    private var volume: Omissible<Int> = Omissible.omitted()
    private var paused: Omissible<Boolean> = Omissible.omitted()
    private var filters: Omissible<Filters> = Omissible.omitted()
    private var state: Omissible<VoiceState> = Omissible.omitted()
    private var noReplace = false

    override fun setTrack(track: Track?): PlayerUpdateBuilder {
        this.trackUpdate = PlayerUpdateTrack(
            encoded = Omissible.of(track?.encoded),
            userData = track?.userData?.toKotlin().toOmissible()
        ).toOmissible()

        this.position = track?.info?.position.toOmissible()

        return this
    }

    override fun updateTrack(update: PlayerUpdateTrack): PlayerUpdateBuilder {
        this.trackUpdate = update.toOmissible()

        return this
    }

    override fun stopTrack(): PlayerUpdateBuilder {
        this.trackUpdate = PlayerUpdateTrack(
            encoded = Omissible.of(null),
        ).toOmissible()
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

    override fun omitEndTime(): PlayerUpdateBuilder {
        this.endTime = Omissible.omitted()
        return this
    }

    override fun setVolume(volume: Int): PlayerUpdateBuilder {
        if (volume < 0 || volume > 1000) {
            throw IllegalArgumentException("Volume must not be less than 0 or greater than 1000")
        }

        this.volume = volume.toOmissible()
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
        if (state.sessionId.isEmpty() || state.endpoint.isEmpty() || state.token.isEmpty()) {
            throw VoiceStateException("Voice state is missing sessionId, endpoint, or token: $state")
        }

        this.state = state.toOmissible()
        return this
    }

    fun setNoReplace(noReplace: Boolean): PlayerUpdateBuilder {
        this.noReplace = noReplace
        return this
    }

    internal fun applyBuilder(builder: PlayerUpdateBuilder): PlayerUpdateBuilder {
        this.trackUpdate = builder.trackUpdate
        this.position = builder.position
        this.endTime = builder.endTime
        this.volume = builder.volume
        this.paused = builder.paused
        this.filters = builder.filters
        this.state = builder.state
        this.noReplace = builder.noReplace

        return this
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun build() = PlayerUpdate(
        track = trackUpdate,
        position = position,
        endTime = endTime,
        volume = volume,
        paused = paused,
        filters = filters,
        voice = state
    )

    override fun subscribe(actual: CoreSubscriber<in LavalinkPlayer>) {
        node.rest.updatePlayer(build(), guildId, noReplace)
            .map { it.toLavalinkPlayer(node) }
            .doOnSuccess {
                // Update player in cache
                node.playerCache[guildId] = it
            }
            .subscribe(actual)
    }
}
