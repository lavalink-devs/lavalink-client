package dev.arbjerg.lavalink.internal

import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.internal.error.RestException
import dev.arbjerg.lavalink.protocol.v4.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
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

class LavalinkRestClient(val node: LavalinkNode) {
    private val client = OkHttpClient()

    private val baseUrl = node.baseUri.replace("ws://", "http://")
        .replace("wss://", "https://")

    fun getPlayers(): Mono<Players> {
        return newRequest {
            url("$baseUrl/sessions/${node.sessionId}/players")
        }.toMono()
    }

    fun getPlayer(guildId: Long): Mono<Player> {
        return newRequest {
            url("$baseUrl/sessions/${node.sessionId}/players/$guildId")
        }.toMono()
    }

    fun updatePlayer(player: PlayerUpdate, guildId: Long, noReplace: Boolean = false): Mono<Player> {
        return newRequest {
            url("$baseUrl/sessions/${node.sessionId}/players/$guildId?noReplace=$noReplace")
            patch(json.encodeToString(player).toRequestBody("application/json".toMediaType()))
        }.toMono()
    }

    fun destroyPlayer(guildId: Long): Mono<Unit> {
        return newRequest {
            url("$baseUrl/sessions/${node.sessionId}/players/$guildId")
            delete()
        }.toMono()
    }

    fun loadItem(identifier: String): Mono<LoadResult> {
        val encId = URLEncoder.encode(identifier, StandardCharsets.UTF_8)

        return newRequest {
            url("$baseUrl/loadtracks?identifier=$encId")
        }.toMono()
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
        return Mono.create { sink ->
            sink.onCancel {
                // try to cancel the request
                this.cancel()
            }
            this.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: Call, e: IOException) {
                    sink.error(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { res ->

                        res.body?.use { body ->
                            if (res.code > 299) {
                                val error = json.decodeFromString<Error>(body.string())

                                sink.error(RestException(error))
                                return
                            }

                            if (res.code == 204) {
                                sink.success()
                                return
                            }

                            val parsed = json.decodeFromString<T>(body.string())

                            sink.success(parsed)
                        }
                    }

                }
            })
        }
    }
}
