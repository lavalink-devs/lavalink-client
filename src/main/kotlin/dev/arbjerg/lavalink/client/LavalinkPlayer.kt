package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.internal.LavalinkRestClient
import dev.arbjerg.lavalink.protocol.v4.*

class LavalinkPlayer(private val rest: LavalinkRestClient, protocolPlayer: Player) : IUpdatablePlayer {
    val guildId = protocolPlayer.guildId.toLong()

    /**
     * Gets the current track that is playing on the player.
     *
     * To get the current position of the track, use [state]
     */
    val track = protocolPlayer.track
    val volume = protocolPlayer.volume
    val paused = protocolPlayer.paused

    /**
     * Gets the current state of the player.
     * See [PlayerState] for more info.
     */
    var state = protocolPlayer.state
        internal set
    val voiceState = protocolPlayer.voice
    val filters = protocolPlayer.filters

    override fun setEncodedTrack(encodedTrack: String?) = PlayerUpdateBuilder(rest, guildId)
        .setEncodedTrack(encodedTrack)

    override fun clearEncodedTrack() = PlayerUpdateBuilder(rest, guildId)
        .clearEncodedTrack()

    override fun setIdentifier(identifier: String?) = PlayerUpdateBuilder(rest, guildId)
        .setIdentifier(identifier)

    override fun setPosition(position: Long?) = PlayerUpdateBuilder(rest, guildId)
        .setPosition(position)

    override fun setEndTime(endTime: Long?) = PlayerUpdateBuilder(rest, guildId)
        .setEndTime(endTime)

    override fun clearEndTime() = PlayerUpdateBuilder(rest, guildId)
        .clearEndTime()

    override fun setVolume(volume: Int) = PlayerUpdateBuilder(rest, guildId)
        .setVolume(volume)

    override fun setPaused(paused: Boolean) = PlayerUpdateBuilder(rest, guildId)
        .setPaused(paused)

    override fun setFilters(filters: Filters) = PlayerUpdateBuilder(rest, guildId)
        .setFilters(filters)

    override fun setVoiceState(state: VoiceState) = PlayerUpdateBuilder(rest, guildId)
        .setVoiceState(state)

}
