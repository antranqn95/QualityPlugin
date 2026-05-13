package com.example.sampleapp

class PlaylistRepository {

    private val cache = mutableMapOf<String, List<Channel>>()

    fun getChannels(categoryId: String): List<Channel> {
        return cache[categoryId] ?: emptyList()
    }

    fun saveChannels(categoryId: String, channels: List<Channel>) {
        if (categoryId.isBlank()) throw IllegalArgumentException("categoryId must not be blank")
        cache[categoryId] = channels
    }

    fun searchChannels(query: String): List<Channel> {
        if (query.length < 2) return emptyList()
        val q = query.lowercase()
        return cache.values.flatten().filter { it.name.lowercase().contains(q) }
    }

    fun deleteCategory(categoryId: String): Boolean {
        return cache.remove(categoryId) != null
    }

    fun clearAll() {
        cache.clear()
    }

    fun getTotalChannelCount(): Int = cache.values.sumOf { it.size }

    fun isCached(categoryId: String): Boolean = cache.containsKey(categoryId)
}
