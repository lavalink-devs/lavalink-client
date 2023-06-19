package dev.arbjerg.lavalink.internal

import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.internal.error.RestException
import dev.arbjerg.lavalink.protocol.v4.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import reactor.core.publisher.Mono
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

class LavalinkRestClient(val node: LavalinkNode) {
    private val client = OkHttpClient()

    private val baseUrl = node.baseUri.replace("ws://", "http://")
        .replace("wss://", "https://")

    fun getPlayers(): Mono<Players> {
        // GET /v4/sessions/{sessionId}/players
        TODO("Not yet implemented")
    }

    fun getPlayer(guildId: Long): Mono<Player> {
        // GET /v4/sessions/{sessionId}/players/{guildId}
        // Keep track of players locally and create one if needed?

        return newRequest {
            url("$baseUrl/sessions/${node.sessionId}/players/$guildId")
        }.toMono()
    }

    fun updatePlayer(player: PlayerUpdate, guildId: Long, noReplace: Boolean = false): Mono<Player> {
        return newRequest {
            url("$baseUrl/sessions/${node.sessionId}/players/$guildId?noReplace=$noReplace")
            patch(json.encodeToString(player).toRequestBody("application/json".toMediaType()))
        }.toMono<Player>()
    }

    fun destroyPlayer(guildId: Long): Mono<Unit> {
        // DELETE /v4/sessions/{sessionId}/players/{guildId}
        TODO("Not yet implemented")
    }

    fun loadItem(identifier: String): Mono<LoadResult> {
        val encId = URLEncoder.encode(identifier, StandardCharsets.UTF_8)

        return newRequest {
            url("$baseUrl/loadtracks?identifier=$encId")
        }.toMono<LoadResult>()
    }

    private fun newRequest(configure: Request.Builder.() -> Request.Builder): Call {
        val builder = configure(
            Request.Builder()
                .addHeader("Authorization", node.password)
                .get()
        )

        return client.newCall(builder.build())
    }

    private inline fun <reified T> Call.toMono(): Mono<T> {
        val future = CompletableFuture<T>()

        this.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->

                    res.body?.use { body ->
                        if (res.code != 200) {
                            val error = json.decodeFromString<Error>(body.string())

                            future.completeExceptionally(RestException(error))
                            return@use
                        }

                        val parsed = json.decodeFromString<T>(body.string())

                        future.complete(parsed)
                    }
                }

            }
        })

        return Mono.fromFuture(future)
    }
}
