package dev.arbjerg.lavalink.client.protocol

import dev.arbjerg.lavalink.internal.fromJsonElement
import dev.arbjerg.lavalink.protocol.v4.deserialize
import dev.arbjerg.lavalink.protocol.v4.json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import dev.arbjerg.lavalink.protocol.v4.Track as ProtocolTrack

class Track internal constructor(internal var internalTrack: ProtocolTrack) {
    val info = internalTrack.info
    val pluginInfo = internalTrack.pluginInfo

    fun setUserData(userData: Any) {
        internalTrack = internalTrack.copyWithUserData(
            json.encodeToJsonElement(userData) as JsonObject
        )
    }

    fun <T> getUserData(klass: Class<T>): T {
        return fromJsonElement(internalTrack.userData, klass)
    }
}
