package dev.arbjerg.lavalink.internal

import com.fasterxml.jackson.databind.ObjectMapper
import dev.arbjerg.lavalink.protocol.v4.json
import kotlinx.serialization.json.JsonElement


private val objectMapper = ObjectMapper()

fun toJsonElement(obj: Any): JsonElement {
    val jsonString = objectMapper.writeValueAsString(obj)

    return json.parseToJsonElement(jsonString)
}

fun <T> fromJsonElement(jsonElement: JsonElement, klass: Class<T>): T {
    val stringValue = jsonElement.toString()

    return objectMapper.readValue(stringValue, klass)
}
