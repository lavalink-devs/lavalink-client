package dev.arbjerg.lavalink.client

import dev.arbjerg.lavalink.client.player.LoadFailed
import dev.arbjerg.lavalink.client.player.PlaylistLoaded
import dev.arbjerg.lavalink.client.player.SearchResult
import dev.arbjerg.lavalink.client.player.TrackLoaded
import java.util.function.Consumer

/**
 * Helper class for creating an [AbstractAudioLoadResultHandler] using only methods that can be passed as lambdas.
 *
 * @param trackLoadedConsumer gets called when a track has loaded
 * @param playlistLoadedConsumer gets called when a playlist has loaded
 * @param searchResultConsumer gets called when a search result has loaded
 * @param noMatchesHandler gets called when there are no matches for your input
 * @param loadFailedConsumer gets called in case of a load failure
 */
class FunctionalLoadResultHandler @JvmOverloads constructor(
    private val trackLoadedConsumer: Consumer<TrackLoaded>?,
    private val playlistLoadedConsumer: Consumer<PlaylistLoaded>? = null,
    private val searchResultConsumer: Consumer<SearchResult>? = null,
    private val noMatchesHandler: Runnable? = null,
    private val loadFailedConsumer: Consumer<LoadFailed>? = null,
) : AbstractAudioLoadResultHandler() {

    override fun ontrackLoaded(result: TrackLoaded) {
        trackLoadedConsumer?.accept(result)
    }

    override fun onPlaylistLoaded(result: PlaylistLoaded) {
        playlistLoadedConsumer?.accept(result)
    }

    override fun onSearchResultLoaded(result: SearchResult) {
        searchResultConsumer?.accept(result)
    }

    override fun noMatches() {
        noMatchesHandler?.run()
    }

    override fun loadFailed(result: LoadFailed) {
        loadFailedConsumer?.accept(result)
    }
}
