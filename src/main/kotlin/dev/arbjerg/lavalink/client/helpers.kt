@file:JvmName("Helpers")

package dev.arbjerg.lavalink.client

import java.util.Base64

fun getUserIdFromToken(token: String): Long {
    val parts = token.split(".")

    if (parts.size != 3) {
        throw IllegalArgumentException("Token is not a valid bot token. A valid token has the following format: \"{part}.{part}.{part}\"")
    }

    try {
        val uIdStr = String(Base64.getDecoder().decode(parts[0]))

        return uIdStr.toLong()
    } catch (e: Exception) {
        throw IllegalArgumentException("Decoding failed: ${e.message}", e)
    }
}

