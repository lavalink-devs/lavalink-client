package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.internal.LavalinkRestClient
import dev.arbjerg.lavalink.protocol.v4.Filters
import dev.arbjerg.lavalink.protocol.v4.PlayerState
import dev.arbjerg.lavalink.protocol.v4.VoiceState
import reactor.core.publisher.Mono

// Represents a "link"
class LavalinkPlayer(private val rest: LavalinkRestClient) {

    fun getState(): Mono<PlayerState> {
        TODO("Not yet implemented")
    }

    fun setEncodedTrack(encodedTrack: String?) = PlayerUpdateBuilder(rest)
        .setEncodedTrack(encodedTrack)

    fun setIdentifier(identifier: String) = PlayerUpdateBuilder(rest)
        .setIdentifier(identifier)

    fun setPosition(position: Long) = PlayerUpdateBuilder(rest)
        .setPosition(position)

    fun steEndTime(endTime: Long?) = PlayerUpdateBuilder(rest)
        .setEndTime(endTime)

    fun setVolume(volume: Double) = PlayerUpdateBuilder(rest)
        .setVolume(volume)

    fun setPaused(paused: Boolean) = PlayerUpdateBuilder(rest)
        .setPaused(paused)

    fun setFilters(filters: Filters) = PlayerUpdateBuilder(rest)
        .setFilters(filters)

    fun setVoice(voice: VoiceState) = PlayerUpdateBuilder(rest)
        .setVoice(voice)

}
