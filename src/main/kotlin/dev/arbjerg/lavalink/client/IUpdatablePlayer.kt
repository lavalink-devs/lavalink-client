package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.protocol.Track
import dev.arbjerg.lavalink.protocol.v4.Filters
import dev.arbjerg.lavalink.protocol.v4.PlayerUpdateTrack
import dev.arbjerg.lavalink.protocol.v4.VoiceState
import dev.arbjerg.lavalink.client.protocol.TrackUpdateBuilder

interface IUpdatablePlayer {
    /**
     * Shortcut for setting the encoded track. This will also send any user-data supplied.
     *
     * @param track The track to apply to this builder
     *
     * @return The updated builder, useful for chaining
     */
    fun setTrack(track: Track?): PlayerUpdateBuilder

    /**
     * Allows you to set the track via the [TrackUpdateBuilder].
     * To stop the player, you can use [stopTrack] or [setTrack] with a null track.
     *
     * @param update the update params created via the [TrackUpdateBuilder].
     *
     * @return The updated builder, useful for chaining
     */
    fun updateTrack(update: TrackUpdateBuilder): PlayerUpdateBuilder {
        return updateTrack(update.build())
    }

    /**
     * Allows you to set the track via the [TrackUpdateBuilder].
     * To stop the player, you can use [stopTrack] or [setTrack] with a null track.
     *
     * @param update the update params created via the [TrackUpdateBuilder].
     *
     * @return The updated builder, useful for chaining
     */
    fun updateTrack(update: PlayerUpdateTrack): PlayerUpdateBuilder

    /**
     * @deprecated Use [setTrack] instead.
     *
     * @return The updated builder, useful for chaining
     */
    @Deprecated(
        message = "Use setTrack instead",
        replaceWith = ReplaceWith("setTrack(track)")
    )
    fun applyTrack(track: Track?): PlayerUpdateBuilder {
        return setTrack(track)
    }

    /**
     * Shortcut for setting the encoded track to {@code null}. This will also clear the user data.
     *
     * @return The updated builder, useful for chaining
     */
    fun stopTrack(): PlayerUpdateBuilder

    /**
     * Sets the encoded track to be played.
     * This will override the identifier and track user data if they were previously set.
     *
     * @param encodedTrack The encoded track to be played. Set it to {@code null} to make the player stop playing.
     *
     * @return The updated builder, useful for chaining
     *
     * @deprecated Use [updateTrack] with the [TrackUpdateBuilder] instead.
     */
    fun setEncodedTrack(encodedTrack: String?): PlayerUpdateBuilder

    /**
     * Omits the encoded track field from being sent to the node during updates.
     *
     * @return The updated builder, useful for chaining
     *
     * @deprecated Use [updateTrack] with the [TrackUpdateBuilder] instead.
     */
    fun omitEncodedTrack(): PlayerUpdateBuilder

    /**
     * Set the identifier on the player.
     * This will override the encoded track and user data if they were previously set.
     *
     *
     * @param identifier the identifier to be played
     *
     * @return The updated builder, useful for chaining
     *
     * @deprecated Use [updateTrack] with the [TrackUpdateBuilder] instead.
     */
    fun setIdentifier(identifier: String?): PlayerUpdateBuilder

    /**
     * Update the position of the player.
     *
     * @param position The new position of the player. Set it to `null` to exclude this field from being sent with an update.
     *
     * @return The updated builder, useful for chaining
     */
    fun setPosition(position: Long?): PlayerUpdateBuilder

    /**
     * Update the end time of the track.
     *
     * @param endTime The new end time of the track. Set it to `null` to exclude this field from being sent with an update.
     *
     * @return The updated builder, useful for chaining
     */
    fun setEndTime(endTime: Long?): PlayerUpdateBuilder

    /**
     * Omits the end time from being sent to the node during updates.
     *
     * @return The updated builder, useful for chaining
     */
    fun omitEndTime(): PlayerUpdateBuilder

    /**
     * Update the volume of the player.
     * While you could use the filters to set volume as well, do note that that is float based (1.0f is 100% volume)
     * and takes the time of your buffer size to apply. This method updates the volume instantly after the update is sent out.
     *
     * @param volume The new player volume, value is between 0 and 1000 where 100 is 100% (default) volume.
     *
     * @return The updated builder, useful for chaining
     */
    fun setVolume(volume: Int): PlayerUpdateBuilder

    /**
     * Update the paused state of the player.
     *
     * @param paused Whether the player should be paused or not.
     *
     * @return The updated builder, useful for chaining
     */
    fun setPaused(paused: Boolean): PlayerUpdateBuilder

    /**
     * Update the filters for the player.
     * Please use [setVolume] to update the player's volume instead. Setting the volume via filters is
     * float based (1.0f is 100% volume) and takes the time of your buffer size to apply.
     *
     * @param filters The new filters to apply to the player. You can use the [dev.arbjerg.lavalink.client.protocol.FilterBuilder] to easily create this object.
     *
     * @return The updated builder, useful for chaining
     */
    fun setFilters(filters: Filters): PlayerUpdateBuilder

    /**
     * Update the voice state for the player.<br>
     * <strong>IMPORTANT:</strong> Only ever use [Link.onVoiceServerUpdate] to update the voice server as this sets the state of the link to [LinkState.CONNECTING]
     *
     * @return The updated builder, useful for chaining
     */
    fun setVoiceState(state: VoiceState): PlayerUpdateBuilder
}
