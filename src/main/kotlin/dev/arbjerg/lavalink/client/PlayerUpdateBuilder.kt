package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.internal.LavalinkRestClient
import dev.arbjerg.lavalink.protocol.v4.*
import kotlin.math.max
import kotlin.math.min

// TODO: where to put "noReplace"
class PlayerUpdateBuilder(private val rest: LavalinkRestClient) {
    private var encodedTrack: Omissible<String?> = Omissible.omitted()
    private var identifier: Omissible<String> = Omissible.omitted()
    private var position: Omissible<Long> = Omissible.omitted()
    private var endTime: Omissible<Long?> = Omissible.omitted()
    private var volume: Omissible<Int> = Omissible.omitted()
    private var paused: Omissible<Boolean> = Omissible.omitted()
    private var filters: Omissible<Filters> = Omissible.omitted()
    private var voice: Omissible<VoiceState> = Omissible.omitted()

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

    fun setVolume(volume: Double): PlayerUpdateBuilder {
        val mappedVolume = (min(10.0, max(0.0, volume)) * 100).toInt()
        this.volume = mappedVolume.toOmissible()
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

    fun setVoice(voice: VoiceState): PlayerUpdateBuilder {
        this.voice = voice.toOmissible()
        return this
    }

    fun build() = PlayerUpdate(
        encodedTrack, identifier, position, endTime, volume, paused, filters, voice
    )

    // REST returns player object
    fun asMono() = rest.updatePlayer(build())
}
