package dev.arbjerg.lavalink.internal

import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.client.http.HttpBuilder
import dev.arbjerg.lavalink.internal.error.RestException
import dev.arbjerg.lavalink.protocol.v4.*
import kotlinx.serialization.encodeToString
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import reactor.core.publisher.Mono
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class LavalinkRestClient(val node: LavalinkNode) {
    fun getPlayers(): Mono<Players> {
        return newRequest {
            path("/v4/sessions/${node.sessionId}/players")
        }.toMono()
    }

    fun getPlayer(guildId: Long): Mono<Player> {
        return newRequest {
            path("/v4/sessions/${node.sessionId}/players/$guildId")
        }.toMono()
    }

    fun updatePlayer(player: PlayerUpdate, guildId: Long, noReplace: Boolean = false): Mono<Player> {
        return newRequest {
            path("/v4/sessions/${node.sessionId}/players/$guildId?noReplace=$noReplace")
            patch(json.encodeToString(player).toRequestBody("application/json".toMediaType()))
        }.toMono()
    }

    fun destroyPlayer(guildId: Long): Mono<Unit> {
        return newRequest {
            path("/v4/sessions/${node.sessionId}/players/$guildId")
            delete()
        }.toMono()
    }

    fun loadItem(identifier: String): Mono<LoadResult> {
        val encId = URLEncoder.encode(identifier, StandardCharsets.UTF_8)

        return newRequest {
            path("/v4/loadtracks?identifier=$encId")
        }.toMono()
    }

    fun decodeTrack(encoded: String): Mono<Track> {
        return newRequest {
            path("/v4/decodetrack?encodedTrack=$encoded")
        }.toMono()
    }

    fun decodeTracks(encoded: List<String>): Mono<Tracks> {
        return newRequest {
            path("/v4/decodetracks")
            post(json.encodeToString(encoded).toRequestBody("application/json".toMediaType()))
        }.toMono()
    }

    fun getNodeInfo(): Mono<Info> {
        return newRequest {
            path("/v4/info")
        }.toMono()
    }

    fun patchSession(session: Session): Mono<Session> {
        return newRequest {
            path("/v4/sessions/${node.sessionId}")
            patch(json.encodeToString(session).toRequestBody("application/json".toMediaType()))
        }.toMono()
    }

    fun getSession(): Mono<Session> {
        return newRequest {
            path("/v4/sessions/${node.sessionId}")
            // Using patch with an empty object is a dirty hack because GET is not supported for this resource
            // 7 years younger me should have known better ~Freya
            patch("{}".toRequestBody("application/json".toMediaType()))
        }.toMono()
    }

    /**
     * Make a request to the lavalink node. This is internal to keep it looking nice in kotlin. Java compatibility is in the node class.
     */
    internal fun newRequest(configure: HttpBuilder.() -> HttpBuilder): Call {
        val requestBuilder = Request.Builder()
            .addHeader("Authorization", node.password)
            .get()
        val builder = configure(HttpBuilder(requestBuilder))

        return node.httpClient.newCall(builder.finalizeUrl(node.baseUri).build())
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
