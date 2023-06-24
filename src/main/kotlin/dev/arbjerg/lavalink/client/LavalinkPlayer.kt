package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.internal.LavalinkRestClient
import dev.arbjerg.lavalink.protocol.v4.*

// Represents a "link"
class LavalinkPlayer(private val rest: LavalinkRestClient, protocolPlayer: Player) {
    val guildId = protocolPlayer.guildId.toLong()
    val track = protocolPlayer.track
    val volume = protocolPlayer.volume
    val paused = protocolPlayer.paused
    var state = protocolPlayer.state
        internal set
    val voiceState = protocolPlayer.voice
    val filters = protocolPlayer.filters

    fun setEncodedTrack(encodedTrack: String?) = PlayerUpdateBuilder(rest, guildId)
        .setEncodedTrack(encodedTrack)

    fun setIdentifier(identifier: String) = PlayerUpdateBuilder(rest, guildId)
        .setIdentifier(identifier)

    fun setPosition(position: Long) = PlayerUpdateBuilder(rest, guildId)
        .setPosition(position)

    fun setEndTime(endTime: Long?) = PlayerUpdateBuilder(rest, guildId)
        .setEndTime(endTime)

    /**
     * While you could use the filters to set volume as well, do note that that is float based (1.0f is 100% volume)
     * and takes the time of your buffer size to apply. This method updates the volume instantly after the update is sent out.
     *
     * @param volume The new player volume, value is between 0 and 1000 where 100 is 100% (default) volume.
     */
    fun setVolume(volume: Int) = PlayerUpdateBuilder(rest, guildId)
        .setVolume(volume)

    fun setPaused(paused: Boolean) = PlayerUpdateBuilder(rest, guildId)
        .setPaused(paused)

    fun setFilters(filters: Filters) = PlayerUpdateBuilder(rest, guildId)
        .setFilters(filters)

    fun setVoiceState(state: VoiceState) = PlayerUpdateBuilder(rest, guildId)
        .setVoiceState(state)

}
