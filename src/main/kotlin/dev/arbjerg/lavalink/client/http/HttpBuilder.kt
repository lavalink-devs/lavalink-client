package dev.arbjerg.lavalink.client.http

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URL

/**
 * Utility class assisting with making custom HTTP requests from a client.
 */
class HttpBuilder(private val internalBuilder: Request.Builder) : Request.Builder() {
    private var path: String = ""

    /**
     * Set the path for this request
     *
     * @param path the path for the request to make, this will be prefixed with the base url of the node (E.G. `http://localhost:2333/v4`)
     *
     * @return The current builder instance, useful for chaining calls
     */
    fun path(path: String): HttpBuilder {
        this.path = path
        return this
    }

    /**
     * Prefixes the path with the base url of the node (E.G. `http://localhost:2333/v4`)
     *
     * @param baseUrl The base url of the node.
     *
     * @return The current builder instance, useful for chaining calls
     */
    internal fun finalizeUrl(baseUrl: String): HttpBuilder {
        internalBuilder.url(baseUrl + path)
        return this
    }

    override fun addHeader(name: String, value: String): HttpBuilder {
        internalBuilder.addHeader(name, value)
        return this
    }

    override fun build() = internalBuilder.build()

    override fun cacheControl(cacheControl: CacheControl): HttpBuilder {
        internalBuilder.cacheControl(cacheControl)
        return this
    }

    override fun delete(body: RequestBody?): HttpBuilder {
        internalBuilder.delete(body)
        return this
    }

    override fun get(): HttpBuilder {
        internalBuilder.get()
        return this
    }

    override fun head(): HttpBuilder {
        internalBuilder.head()
        return this
    }

    override fun header(name: String, value: String): HttpBuilder {
        internalBuilder.header(name, value)
        return this
    }

    override fun headers(headers: Headers): HttpBuilder {
        internalBuilder.headers(headers)
        return this
    }

    override fun method(method: String, body: RequestBody?): HttpBuilder {
        internalBuilder.method(method, body)
        return this
    }

    override fun patch(body: RequestBody): HttpBuilder {
        internalBuilder.patch(body)
        return this
    }

    override fun post(body: RequestBody): HttpBuilder {
        internalBuilder.post(body)
        return this
    }

    override fun put(body: RequestBody) : HttpBuilder {
        internalBuilder.put(body)
        return this
    }

    override fun removeHeader(name: String): HttpBuilder {
        internalBuilder.removeHeader(name)
        return this
    }

    override fun <T> tag(type: Class<in T>, tag: T?): HttpBuilder {
        internalBuilder.tag(type, tag)
        return this
    }

    override fun tag(tag: Any?): HttpBuilder {
        internalBuilder.tag(tag)
        return this
    }

    override fun url(url: URL): HttpBuilder {
        this.path = url.toString().toHttpUrl().encodedPath
        return this
    }

    override fun url(url: String): HttpBuilder {
        this.path = url.toHttpUrl().encodedPath
        return this
    }

    override fun url(url: HttpUrl): HttpBuilder {
        this.path = url.encodedPath
        return this
    }
}
