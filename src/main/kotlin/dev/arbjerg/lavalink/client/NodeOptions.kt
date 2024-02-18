package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.loadbalancing.IRegionFilter
import dev.arbjerg.lavalink.internal.TIMEOUT_MS
import java.net.URI

data class NodeOptions private constructor(val name: String,
                       val serverUri: URI,
                       val password: String,
                       val regionFilter: IRegionFilter?,
                       val httpTimeout: Long) {
    data class Builder(
        private var name: String? = null,
        private var serverUri: URI? = null,
        private var password: String? = null,
        private var regionFilter: IRegionFilter? = null,
        private var httpTimeout: Long = TIMEOUT_MS,
    ) {
        fun setName(name: String) = apply { this.name = name }

        /**
         * Sets the server URI of the Lavalink Node.
         * @param serverUriString - String representation of server uri
         */
        fun setServerUri(serverUriString: String) = apply { this.serverUri = URI(serverUriString) }
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

        fun build(): NodeOptions {
            requireNotNull(name) { "name is required" }
            requireNotNull(serverUri) { "serverUri is required" }
            requireNotNull(password) { "password is required" }

            return NodeOptions(
                name!!,
                serverUri!!,
                password!!,
                regionFilter,
                httpTimeout)
        }
    }
}
