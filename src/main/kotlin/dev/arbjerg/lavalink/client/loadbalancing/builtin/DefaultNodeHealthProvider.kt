package dev.arbjerg.lavalink.client.loadbalancing.builtin

import dev.arbjerg.lavalink.client.LavalinkNode
import dev.arbjerg.lavalink.client.loadbalancing.MAX_ERROR
import dev.arbjerg.lavalink.internal.loadbalancing.MetricService
import dev.arbjerg.lavalink.internal.loadbalancing.MetricType
import dev.arbjerg.lavalink.protocol.v4.Message
import kotlin.math.pow

// Clearing them on a timer sucks, here's some ideas from freya:
/*
    You could keep a history and delete older entries (linked list with max entries?)
    Or you could keep time series with a counter counting up, comparing it to the count as old as half an hour ago (on every minute add a new entry to keep track of the events we had in that minute?
*/
// Tracks stuck per minute
// Track exceptions per minute
// loads failed per minute
data class DefaultNodeHealthProvider(val node: LavalinkNode): INodeHealthProvider {
    private val metricService = MetricService()

    override fun handleTrackEvent(event: Message.EmittedEvent) {
        when (event) {
            is Message.EmittedEvent.TrackStartEvent -> {
                metricService.trackMetric(MetricType.LOAD_ATTEMPT)
            }

            is Message.EmittedEvent.TrackEndEvent -> {
                if (event.reason == Message.EmittedEvent.TrackEndEvent.AudioTrackEndReason.LOAD_FAILED) {
                    metricService.trackMetric(MetricType.LOAD_FAILED)
                }
            }

            is Message.EmittedEvent.TrackExceptionEvent -> {
                metricService.trackMetric(MetricType.TRACK_EXCEPTION)
            }

            is Message.EmittedEvent.TrackStuckEvent -> {
                metricService.trackMetric(MetricType.TRACK_STUCK)
            }

            else -> {
                // Ignore everything else
            }
        }
    }

    override fun calculateTotalHealthPenalty(): Int {
        val stats = node.stats

        if (!node.available || stats == null) {
            return MAX_ERROR
        }

        val metrics = metricService.getCurrentMetrics()
        val loadsAttempted = metrics[MetricType.LOAD_ATTEMPT] ?: 0
        val loadsFailed = metrics[MetricType.LOAD_FAILED] ?: 0

        // When the node fails to load anything, we consider it to have the highest penalty
        if (loadsAttempted > 0 && loadsAttempted == loadsFailed) {
            return MAX_ERROR
        }

        // The way we calculate penalties is heavily based on the original Lavalink client.

        // This will serve as a rule of thumb. 1 playing player = 1 penalty point
        val playerPenalty = stats.playingPlayers

        val cpuPenalty = (1.05.pow(100 * stats.cpu.systemLoad) * 10 - 10).toInt()

        val frames = stats.frameStats
        var deficitFramePenalty = 0
        var nullFramePenalty = 0

        // frame stats are per minute.
        // -1 or null means we don't have any frame stats. This is normal for very young nodes
        if (frames != null && frames.deficit != -1) {
            deficitFramePenalty = ( 1.03f.pow(500f * (frames.deficit / 3000f)) * 600 - 600 ).toInt()
            nullFramePenalty = ( 1.03f.pow(500f * (frames.nulled / 3000f)) * 600 - 600 ).toInt()
            nullFramePenalty *= 2
        }

        val tracksStuck = metrics[MetricType.TRACK_STUCK] ?: 0
        val trackExceptions = metrics[MetricType.TRACK_EXCEPTION] ?: 0

        // This is where we differ from the original client, penalties for failures.
        val trackStuckPenalty = tracksStuck * 100 - 100
        val trackExceptionPenalty = trackExceptions * 10 - 10
        val loadFailedPenalty = if (loadsFailed > 0) loadsFailed / loadsAttempted else 0

        return playerPenalty + cpuPenalty + deficitFramePenalty + nullFramePenalty + trackStuckPenalty + trackExceptionPenalty + loadFailedPenalty
    }

    override fun isHealthy(): Boolean {
        val metrics = metricService.getCurrentMetrics()
        val loadsAttempted = metrics[MetricType.LOAD_ATTEMPT] ?: 0
        val loadsFailed = metrics[MetricType.LOAD_FAILED] ?: 0

        // When the node fails to load anything, we consider it to be unhealthy
        return !(loadsAttempted > 0 && loadsAttempted == loadsFailed) && node.available
    }
}
