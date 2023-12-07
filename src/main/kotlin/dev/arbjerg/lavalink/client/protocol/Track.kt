package dev.arbjerg.lavalink.client.protocol

import dev.arbjerg.lavalink.internal.fromJsonElement
import dev.arbjerg.lavalink.internal.toJackson
import dev.arbjerg.lavalink.internal.toJsonElement
import kotlinx.serialization.json.JsonObject
import dev.arbjerg.lavalink.protocol.v4.Track as ProtocolTrack

internal fun ProtocolTrack.toCustom() = Track(this)

class Track internal constructor(private var internalTrack: ProtocolTrack) {
    val encoded = internalTrack.encoded
    val rawUserData = internalTrack.userData
    val info = internalTrack.info
    val pluginInfo = internalTrack.pluginInfo.toJackson()

    fun setUserData(userData: Any) {
        internalTrack = internalTrack.copyWithUserData(
            toJsonElement(userData) as JsonObject
        )
    }

    fun <T> getUserData(klass: Class<T>): T {
        return fromJsonElement(internalTrack.userData, klass)
    }
}
