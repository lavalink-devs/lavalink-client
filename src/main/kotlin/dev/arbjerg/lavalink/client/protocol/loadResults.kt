package dev.arbjerg.lavalink.client.protocol

import dev.arbjerg.lavalink.internal.toJackson
import dev.arbjerg.lavalink.protocol.v4.Exception as ProtocolException
import dev.arbjerg.lavalink.protocol.v4.LoadResult

open class LavalinkLoadResult

fun LoadResult.toLavalinkLoadResult() = when (this) {
    is LoadResult.TrackLoaded -> TrackLoaded(this)
    is LoadResult.PlaylistLoaded -> PlaylistLoaded(this)
    is LoadResult.SearchResult -> SearchResult(this)
    is LoadResult.NoMatches -> NoMatches()
    is LoadResult.LoadFailed -> LoadFailed(this)
}

class TrackLoaded(result: LoadResult.TrackLoaded) : LavalinkLoadResult() {
    val track = result.data.toCustom()
}

class PlaylistLoaded(result: LoadResult.PlaylistLoaded) : LavalinkLoadResult() {
    val info = result.data.info
    val pluginInfo = result.data.pluginInfo.toJackson()
    val tracks = result.data.tracks.map { it.toCustom() }
}

class NoMatches : LavalinkLoadResult()

class SearchResult(result: LoadResult.SearchResult) : LavalinkLoadResult() {
    val tracks = result.data.tracks.map { it.toCustom() }
}

internal fun ProtocolException.toCustom() = TrackException(message, severity, cause)

data class TrackException(
    val message: String?,
    val severity: ProtocolException.Severity,
    val cause: String
)

class LoadFailed(result: LoadResult.LoadFailed) : LavalinkLoadResult() {
    val exception = result.data.toCustom()
}
