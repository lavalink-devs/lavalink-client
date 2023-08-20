package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.protocol.v4.Filters
import dev.arbjerg.lavalink.protocol.v4.Player
import dev.arbjerg.lavalink.protocol.v4.PlayerState
import dev.arbjerg.lavalink.protocol.v4.VoiceState

class LavalinkPlayer(private val node: LavalinkNode, protocolPlayer: Player) : IUpdatablePlayer {
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

    override fun setEncodedTrack(encodedTrack: String?) = PlayerUpdateBuilder(node, guildId)
        .setEncodedTrack(encodedTrack)

    override fun clearEncodedTrack() = PlayerUpdateBuilder(node, guildId)
        .clearEncodedTrack()

    override fun setIdentifier(identifier: String?) = PlayerUpdateBuilder(node, guildId)
        .setIdentifier(identifier)

    override fun setPosition(position: Long?) = PlayerUpdateBuilder(node, guildId)
        .setPosition(position)

    override fun setEndTime(endTime: Long?) = PlayerUpdateBuilder(node, guildId)
        .setEndTime(endTime)

    override fun clearEndTime() = PlayerUpdateBuilder(node, guildId)
        .clearEndTime()

    override fun setVolume(volume: Int) = PlayerUpdateBuilder(node, guildId)
        .setVolume(volume)

    override fun setPaused(paused: Boolean) = PlayerUpdateBuilder(node, guildId)
        .setPaused(paused)

    override fun setFilters(filters: Filters) = PlayerUpdateBuilder(node, guildId)
        .setFilters(filters)

    override fun setVoiceState(state: VoiceState) = PlayerUpdateBuilder(node, guildId)
        .setVoiceState(state)

}
