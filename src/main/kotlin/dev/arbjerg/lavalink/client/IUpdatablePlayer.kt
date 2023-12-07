package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.protocol.Track
import dev.arbjerg.lavalink.protocol.v4.Filters
import dev.arbjerg.lavalink.protocol.v4.VoiceState

interface IUpdatablePlayer {
    /**
     * Shortcut for setting the encoded track. This will also send any user-data supplied.
     *
     * @param track The track to apply to this builder
     *
     * @return The updated builder, useful for chaining
     */
    fun setTrack(track: Track?): PlayerUpdateBuilder

    @Deprecated(
        message = "Use setTrack instead",
        replaceWith = ReplaceWith("setTrack(track)")
    )
    fun applyTrack(track: Track?): PlayerUpdateBuilder {
        return setTrack(track)
    }

    /**
     * Shortcut for setting the encoded track to {@code null}. This will also clear the user data.
     */
    fun stopTrack(): PlayerUpdateBuilder

    /**
     * Sets the encoded track to be played.
     *
     * @param encodedTrack The encoded track to be played. Set it to {@code null} to make the player stop playing.
     */
    fun setEncodedTrack(encodedTrack: String?): PlayerUpdateBuilder

    /**
     * Omits the encoded track field from being sent to the node during updates.
     */
    fun omitEncodedTrack(): PlayerUpdateBuilder
    fun setIdentifier(identifier: String?): PlayerUpdateBuilder
    fun setPosition(position: Long?): PlayerUpdateBuilder
    fun setEndTime(endTime: Long?): PlayerUpdateBuilder

    /**
     * Omits the end time from being sent to the node during updates.
     */
    fun omitEndTime(): PlayerUpdateBuilder

    /**
     * While you could use the filters to set volume as well, do note that that is float based (1.0f is 100% volume)
     * and takes the time of your buffer size to apply. This method updates the volume instantly after the update is sent out.
     *
     * @param volume The new player volume, value is between 0 and 1000 where 100 is 100% (default) volume.
     */
    fun setVolume(volume: Int): PlayerUpdateBuilder
    fun setPaused(paused: Boolean): PlayerUpdateBuilder
    fun setFilters(filters: Filters): PlayerUpdateBuilder

    /**
     * Update the voice state for the player.<br>
     * <strong>IMPORTANT:</strong> Only ever use [Link.onVoiceServerUpdate] to update the voice server as this sets the state of the link to [LinkState.CONNECTING]
     */
    fun setVoiceState(state: VoiceState): PlayerUpdateBuilder
}
