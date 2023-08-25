package dev.arbjerg.lavalink.internal.loadbalancing

// 0.75 is the default load factor for LinkedHashMap
class LRUCache<K, V>(private val limit: Int) : LinkedHashMap<K, V>(limit, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > limit
    }
}
