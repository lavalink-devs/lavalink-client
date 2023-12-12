package dev.arbjerg.lavalink.internal

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dev.arbjerg.lavalink.protocol.v4.json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.io.InputStream


private val objectMapper = ObjectMapper()

fun JsonElement.toJackson(): JsonNode = objectMapper.readTree(this.toString())
fun JsonNode.toKotlin(): JsonObject = toJsonElement(this) as JsonObject

fun toJsonElement(obj: Any): JsonElement {
    val jsonString = objectMapper.writeValueAsString(obj)

    return json.parseToJsonElement(jsonString)
}

fun <T> fromRawJson(bytes: InputStream, klass: Class<T>): T {
    return objectMapper.readValue(bytes, klass)
}

fun <T> fromJsonElement(jsonElement: JsonElement, klass: Class<T>): T {
    val stringValue = jsonElement.toString()

    return objectMapper.readValue(stringValue, klass)
}
