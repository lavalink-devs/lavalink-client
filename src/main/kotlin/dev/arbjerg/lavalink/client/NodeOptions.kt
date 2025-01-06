package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.loadbalancing.IRegionFilter
import dev.arbjerg.lavalink.internal.TIMEOUT_MS
import java.net.URI

data class NodeOptions private constructor(val name: String,
                       val serverUri: URI,
                       val password: String,
                       val regionFilter: IRegionFilter?,
                       val httpTimeout: Long,
                       val sessionId: String?) {
    data class Builder(
        private var name: String? = null,
        private var serverUri: URI? = null,
        private var password: String? = null,
        private var regionFilter: IRegionFilter? = null,
        private var httpTimeout: Long = TIMEOUT_MS,
        private var sessionId: String? = null
    ) {
        fun setName(name: String) = apply { this.name = name }

        /**
         * Sets the server URI of the Lavalink Node. If no port is present in the URI, it will be set to 2333.
         * @param serverUriString - String representation of server uri
         */
        fun setServerUri(serverUriString: String) = apply {
            var parsedUri = URI(serverUriString)

            if (parsedUri.port == -1) {
                parsedUri = URI("$serverUriString:2333")
            }

            this.serverUri = parsedUri
        }
        /**
         * Sets the server URI of the Lavalink Node.
         * @param serverUri - Server uri
         */
        fun setServerUri(serverUri: URI) = apply { this.serverUri = serverUri }
        /**
         * Sets the password to access the node.
         * @param password - Server password
         */
        fun setPassword(password: String) = apply { this.password = password }

        /**
         * Sets a region filter on the node for regional load balancing (Default: none)
         */
        fun setRegionFilter(regionFilter: IRegionFilter?) = apply { this.regionFilter = regionFilter }

        /**
         * Sets the http total call timeout. (Default: 10000ms)
         * @param httpTimeout - timeout in ms
         */
        fun setHttpTimeout(httpTimeout: Long) = apply { this.httpTimeout = httpTimeout }

        /**
         * Sets the session ID that the client will use when first connecting to Lavalink. If the given session is still
         *   running on the Lavalink server, the session will be resumed.
         *
         * Defaults to null, which means no attempt to resume will be made.
         */
        fun setSessionId(sessionId: String?) = apply { this.sessionId = sessionId }

        fun build(): NodeOptions {
            requireNotNull(name) { "name is required" }
            requireNotNull(serverUri) { "serverUri is required" }
            requireNotNull(password) { "password is required" }

            return NodeOptions(
                name!!,
                serverUri!!,
                password!!,
                regionFilter,
                httpTimeout,
                sessionId)
        }
    }
}
