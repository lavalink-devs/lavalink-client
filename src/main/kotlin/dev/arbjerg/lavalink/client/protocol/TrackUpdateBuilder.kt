package dev.arbjerg.lavalink.client.protocol

import dev.arbjerg.lavalink.internal.toJsonElement
import dev.arbjerg.lavalink.protocol.v4.Omissible
import dev.arbjerg.lavalink.protocol.v4.PlayerUpdateTrack
import dev.arbjerg.lavalink.protocol.v4.toOmissible
import kotlinx.serialization.json.JsonObject

/**
 * Allows you to update the playing track with only the fields that you wish to update.
 */
class TrackUpdateBuilder {
    private var internalUpdate = PlayerUpdateTrack()

    /**
     * Sets the encoded track to be played.
     * This will override the identifier if previously set.
     *
     * @param encoded The encoded track to be played. Set it to {@code null} to make the player stop playing.
     *
     * @return The updated builder, useful for chaining
     */
    fun setEncoded(encoded: String?) = apply {
        internalUpdate = internalUpdate.copy(
            encoded = Omissible.of(encoded),
            identifier = Omissible.omitted()
        )
    }

    /**
     * Set the identifier on the player.
     * This will override the encoded track if previously set.
     *
     * @param identifier the identifier to be played
     *
     * @return The updated builder, useful for chaining
     */
    fun setIdentifier(identifier: String) = apply {
        internalUpdate = internalUpdate.copy(
            encoded = Omissible.omitted(),
            identifier = Omissible.of(identifier)
        )
    }

    fun setUserData(userData: Any) = apply {
        internalUpdate = internalUpdate.copy(
            userData = (toJsonElement(userData) as JsonObject).toOmissible()
        )
    }

    fun build() = internalUpdate
}
