package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.protocol.v4.LoadResult
import java.util.function.Consumer

abstract class AbstractAudioLoadResultHandler : Consumer<LoadResult> {
    override fun accept(loadResult: LoadResult) {
        when (loadResult) {
            is LoadResult.TrackLoaded -> {
                this.ontrackLoaded(loadResult)
            }

            is LoadResult.PlaylistLoaded -> {
                this.onPlaylistLoaded(loadResult)
            }

            is LoadResult.SearchResult -> {
                this.onSearchResultLoaded(loadResult)
            }

            is LoadResult.NoMatches -> {
                this.noMatches()
            }

            is LoadResult.LoadFailed -> {
                this.loadFailed(loadResult)
            }
        }
    }

    abstract fun ontrackLoaded(result: LoadResult.TrackLoaded)
    abstract fun onPlaylistLoaded(result: LoadResult.PlaylistLoaded)
    abstract fun onSearchResultLoaded(result: LoadResult.SearchResult)
    abstract fun noMatches()
    abstract fun loadFailed(result: LoadResult.LoadFailed)
}
