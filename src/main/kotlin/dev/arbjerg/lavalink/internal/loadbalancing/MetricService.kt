package dev.arbjerg.lavalink.internal.loadbalancing

import java.text.SimpleDateFormat
import java.util.Date

class MetricService {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")

    // timestamp to metric
    private val timeMap = LRUCache<String, MutableMap<MetricType, Int>>(100)

    fun trackMetric(metric: MetricType) {
        val timestamp = dateFormat.format(Date())
        val metricMap = timeMap.getOrPut(timestamp) { mutableMapOf() }

        val currMetric = metricMap[metric] ?: 0

        metricMap[metric] = currMetric + 1

        println(metricMap)
    }

    fun getCurrentMetrics(): Map<MetricType, Int> {
        val metricMap = mutableMapOf<MetricType, Int>()

        // TODO: there's probably a better way to do this
        timeMap.values.forEach {
            it.forEach { (metric, value) ->
                val currMetric = metricMap[metric] ?: 0

                metricMap[metric] = currMetric + value
            }
        }

        return metricMap
    }
}
