package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.internal.toLavalinkPlayer
import dev.arbjerg.lavalink.protocol.v4.*
import kotlinx.serialization.json.JsonObject
import reactor.core.publisher.Mono
import kotlin.math.max
import kotlin.math.min

class PlayerUpdateBuilder internal constructor(private val node: LavalinkNode, private val guildId: Long) : IUpdatablePlayer {
    private var encodedTrack: Omissible<String?> = Omissible.omitted()
    private var identifier: Omissible<String> = Omissible.omitted()
    private var trackUserData: Omissible<JsonObject> = Omissible.Omitted()
    private var position: Omissible<Long> = Omissible.omitted()
    private var endTime: Omissible<Long?> = Omissible.omitted()
    private var volume: Omissible<Int> = Omissible.omitted()
    private var paused: Omissible<Boolean> = Omissible.omitted()
    private var filters: Omissible<Filters> = Omissible.omitted()
    private var state: Omissible<VoiceState> = Omissible.omitted()
    private var noReplace = false

    override fun applyTrack(track: Track?): PlayerUpdateBuilder {
        this.encodedTrack = Omissible.of(track?.encoded)
        this.trackUserData = track?.userData.toOmissible()
        return this
    }

    override fun stopTrack(): PlayerUpdateBuilder {
        this.encodedTrack = Omissible.of(null)
        this.identifier = null.toOmissible()
        this.trackUserData = null.toOmissible()
        return this
    }

    override fun setEncodedTrack(encodedTrack: String?): PlayerUpdateBuilder {
        this.encodedTrack = Omissible.of(encodedTrack)
        return this
    }

    override fun omitEncodedTrack(): PlayerUpdateBuilder {
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

    override fun omitEndTime(): PlayerUpdateBuilder {
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

    internal fun applyBuilder(builder: PlayerUpdateBuilder): PlayerUpdateBuilder {
        this.encodedTrack = builder.encodedTrack
        this.identifier = builder.identifier
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
        track = PlayerUpdateTrack(
            encodedTrack,
            identifier,
            trackUserData,
        ).toOmissible(),
        position = position,
        endTime = endTime,
        volume = volume,
        paused = paused,
        filters = filters,
        voice = state
    )

    fun asMono(): Mono<LavalinkPlayer> {
        return node.rest.updatePlayer(build(), guildId, noReplace)
            .map { it.toLavalinkPlayer(node) }
            .doOnNext {
                // Update player in cache
                node.playerCache[guildId] = it
            }
    }
}
