package dev.arbjerg.lavalink.internal.loadbalancing

import dev.arbjerg.lavalink.internal.METRIC_MAX_HISTORY
import java.text.SimpleDateFormat
import java.util.Date

class MetricService {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")

    // timestamp to metric
    private val timeMap = LRUCache<String, MutableMap<MetricType, Int>>(METRIC_MAX_HISTORY)

    fun trackMetric(metric: MetricType) {
        val timestamp = dateFormat.format(Date())
        val metricMap = timeMap.getOrPut(timestamp) { mutableMapOf() }

        val currMetric = metricMap[metric] ?: 0

        metricMap[metric] = currMetric + 1
    }

    fun getCurrentMetrics(): Map<MetricType, Int> {
        val metricMap = mutableMapOf<MetricType, Int>()

        // there's probably a better way to do this
        timeMap.values.forEach {
            it.forEach { (metric, value) ->
                val currMetric = metricMap[metric] ?: 0

                metricMap[metric] = currMetric + value
            }
        }

        return metricMap
    }

    internal fun resetMetrics() {
        timeMap.clear()
    }
}
