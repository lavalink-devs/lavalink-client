package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.loadbalancing.VoiceRegion
import dev.arbjerg.lavalink.client.protocol.Track
import dev.arbjerg.lavalink.client.protocol.toCustom
import dev.arbjerg.lavalink.protocol.v4.*
import kotlin.math.min

/**
 * Represents a player that is tied to a guild.
 */
class LavalinkPlayer(private val node: LavalinkNode, protocolPlayer: Player) : IUpdatablePlayer {
    val guildId = protocolPlayer.guildId.toLong()

    /**
     * Gets the current track that is playing on the player.
     *
     * To get the current position of the track, use [position].
     */
    var track = protocolPlayer.track?.toCustom()
        internal set

    /**
     * Number between 0 and 1000, where 100 is 100% volume.
     */
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

    val position: Long
        get() {
            val checkedTrack = track
            return when {
                checkedTrack == null -> 0
                paused -> state.position
                else -> min(state.position + (System.currentTimeMillis() - state.time), checkedTrack.info.length)
            }
        }

    val voiceRegion: VoiceRegion?
        get() {
            if (voiceState.endpoint.isBlank()) {
                return null
            }

            return VoiceRegion.fromEndpoint(voiceState.endpoint)
        }

    override fun setTrack(track: Track?) = PlayerUpdateBuilder(node, guildId)
        .setTrack(track)

    override fun updateTrack(update: PlayerUpdateTrack) = PlayerUpdateBuilder(node, guildId)
        .updateTrack(update)

    override fun stopTrack() = PlayerUpdateBuilder(node, guildId)
        .stopTrack()

    override fun setPosition(position: Long?) = PlayerUpdateBuilder(node, guildId)
        .setPosition(position)

    override fun setEndTime(endTime: Long?) = PlayerUpdateBuilder(node, guildId)
        .setEndTime(endTime)

    override fun omitEndTime() = PlayerUpdateBuilder(node, guildId)
        .omitEndTime()

    override fun setVolume(volume: Int) = PlayerUpdateBuilder(node, guildId)
        .setVolume(volume)

    override fun setPaused(paused: Boolean) = PlayerUpdateBuilder(node, guildId)
        .setPaused(paused)

    override fun setFilters(filters: Filters) = PlayerUpdateBuilder(node, guildId)
        .setFilters(filters)

    override fun setVoiceState(state: VoiceState) = PlayerUpdateBuilder(node, guildId)
        .setVoiceState(state)

    // For re-creating the player
    internal fun stateToBuilder() = PlayerUpdateBuilder(node, guildId)
        .setTrack(track)
        .setPosition(position)
        .setEndTime(null) // TODO Should we keep track of the end time?
        .setVolume(volume)
        .setPaused(paused)
        .setFilters(filters)
        .setVoiceState(voiceState)

}
