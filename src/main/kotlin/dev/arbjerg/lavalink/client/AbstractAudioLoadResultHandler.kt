package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.player.*
import java.util.function.Consumer

abstract class AbstractAudioLoadResultHandler : Consumer<LavalinkLoadResult> {
    override fun accept(loadResult: LavalinkLoadResult) {
        when (loadResult) {
            is TrackLoaded -> {
                this.ontrackLoaded(loadResult)
            }

            is PlaylistLoaded -> {
                this.onPlaylistLoaded(loadResult)
            }

            is SearchResult -> {
                this.onSearchResultLoaded(loadResult)
            }

            is NoMatches -> {
                this.noMatches()
            }

            is LoadFailed -> {
                this.loadFailed(loadResult)
            }
        }
    }

    abstract fun ontrackLoaded(result: TrackLoaded)
    abstract fun onPlaylistLoaded(result: PlaylistLoaded)
    abstract fun onSearchResultLoaded(result: SearchResult)
    abstract fun noMatches()
    abstract fun loadFailed(result: LoadFailed)
}
