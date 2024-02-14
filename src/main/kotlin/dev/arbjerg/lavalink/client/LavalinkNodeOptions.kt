package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.loadbalancing.IRegionFilter
import dev.arbjerg.lavalink.internal.TIMEOUT_MS
import java.net.URI

data class LavalinkNodeOptions(val name: String,
                               val serverUri: URI,
                               val password: String,
                               val regionFilter: IRegionFilter?,
                               val httpTimeout: Long) {
    data class Builder(
        var name: String? = null,
        private var serverUri: URI? = null,
        private var password: String? = null,
        private var regionFilter: IRegionFilter? = null,
        private var httpTimeout: Long = TIMEOUT_MS,
    ) {
        fun name(name: String) = apply { this.name = name }
        fun serverUri(serverUriString: String) = apply { this.serverUri = URI(serverUriString) }
        fun serverUri(serverUri: URI) = apply { this.serverUri = serverUri }
        fun password(password: String) = apply { this.password = password }
        fun regionFilter(regionFilter: IRegionFilter?) = apply { this.regionFilter = regionFilter }
        fun httpTimeout(httpTimeout: Long) = apply { this.httpTimeout = httpTimeout }

        fun build(): LavalinkNodeOptions {
            requireNotNull(name) { "name is required" }
            requireNotNull(serverUri) { "serverUri is required" }
            requireNotNull(password) { "password is required" }

            return LavalinkNodeOptions(
                name!!,
                serverUri!!,
                password!!,
                regionFilter,
                httpTimeout)
        }
    }
}
