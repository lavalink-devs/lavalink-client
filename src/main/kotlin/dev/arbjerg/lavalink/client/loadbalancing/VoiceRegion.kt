package dev.arbjerg.lavalink.client.loadbalancing

// Todo: have a voice region group that houses voice regions
enum class VoiceRegion(val endpoint: String) {
    NONE("");

    companion object {
        fun fromEndpoint(endpoint: String): VoiceRegion {
            entries.forEach {
                if (it.endpoint == endpoint) {
                    return it
                }
            }

            return NONE
        }
    }
}
