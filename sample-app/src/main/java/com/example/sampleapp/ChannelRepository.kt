package com.example.sampleapp

interface ChannelDataSource {
    fun fetchChannels(category: String): List<Channel>
    fun fetchChannel(id: String): Channel?
}

class ChannelRepository(private val dataSource: ChannelDataSource) {

    private val cache = mutableMapOf<String, Channel>()

    fun getChannel(id: String): Channel? {
        return cache[id] ?: dataSource.fetchChannel(id)?.also { cache[id] = it }
    }

    fun getChannelsByCategory(category: String): List<Channel> {
        if (category.isBlank()) throw IllegalArgumentException("Category must not be blank")
        return dataSource.fetchChannels(category)
    }

    fun clearCache() {
        cache.clear()
    }

    fun getCachedCount(): Int = cache.size

    fun isCached(channelId: String): Boolean = cache.containsKey(channelId)
}
