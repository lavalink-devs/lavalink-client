package dev.arbjerg.lavalink.client.player

import com.fasterxml.jackson.databind.JsonNode
import dev.arbjerg.lavalink.internal.fromJsonElement
import dev.arbjerg.lavalink.internal.toJackson
import dev.arbjerg.lavalink.internal.toJsonElement
import kotlinx.serialization.json.jsonObject
import dev.arbjerg.lavalink.protocol.v4.Track as ProtocolTrack

internal fun ProtocolTrack.toCustom() = Track(this)

class Track internal constructor(private var internalTrack: ProtocolTrack) {
    val encoded = internalTrack.encoded
    val userData: JsonNode
        get() = internalTrack.userData.toJackson()
    val info = internalTrack.info
    val pluginInfo = internalTrack.pluginInfo.toJackson()

    fun setUserData(userData: Any?) {
        internalTrack = internalTrack.copyWithUserData(
            toJsonElement(userData).jsonObject
        )
    }

    fun <T> getUserData(klass: Class<T>): T {
        return fromJsonElement(internalTrack.userData, klass)
    }

    /**
     * Clones this [Track] based on the current [ProtocolTrack] from the server. Custom user data will be cloned as well.
     */
    fun makeClone(): Track {
        val clone = Track(
            internalTrack.copy(
                info = info.copy(position = 0L)
            )
        )

        clone.setUserData(userData)

        return clone
    }
}
