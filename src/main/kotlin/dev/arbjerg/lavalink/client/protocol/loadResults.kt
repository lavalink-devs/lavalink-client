package dev.arbjerg.lavalink.client.protocol

import dev.arbjerg.lavalink.protocol.v4.LoadResult

sealed class LavalinkLoadResult

fun LoadResult.toLavalinkLoadResult() = when (this) {
    is LoadResult.TrackLoaded -> TrackLoaded(this)
    is LoadResult.PlaylistLoaded -> PlaylistLoaded(this)
    is LoadResult.SearchResult -> SearchResult(this)
    is LoadResult.NoMatches -> NoMatches()
    is LoadResult.LoadFailed -> LoadFailed(this)
}

class TrackLoaded(result: LoadResult.TrackLoaded) : LavalinkLoadResult() {
    val track = Track(result.data)
}

class PlaylistLoaded(result: LoadResult.PlaylistLoaded) : LavalinkLoadResult() {
    val info = result.data.info
    val pluginInfo = result.data.pluginInfo
    val tracks = result.data.tracks.map { Track(it) }
}

class NoMatches : LavalinkLoadResult() {}

class SearchResult(result: LoadResult.SearchResult) : LavalinkLoadResult() {
    val tracks = result.data.tracks.map { Track(it) }
}

class LoadFailed(result: LoadResult.LoadFailed) : LavalinkLoadResult() {
    val exception = result.data
}
