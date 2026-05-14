package com.example.sampleapp

class FetchPlaylistUseCase(private val repository: PlaylistRepository) {

    sealed class Result {
        data class Success(val channels: List<Channel>) : Result()
        data class Empty(val category: String) : Result()
        data class Error(val message: String) : Result()
    }

    fun execute(category: String): Result {
        if (category.isBlank()) {
            return Result.Error("Category must not be blank")
        }
        if (!repository.isCached(category)) {
            return Result.Error("Category '$category' not found")
        }
        val channels = repository.getChannels(category)
        return if (channels.isEmpty()) {
            Result.Empty(category)
        } else {
            Result.Success(channels)
        }
    }

    fun search(query: String): Result {
        if (query.length < 2) {
            return Result.Error("Query must be at least 2 characters")
        }
        val channels = repository.searchChannels(query)
        return if (channels.isEmpty()) Result.Empty(query) else Result.Success(channels)
    }
}
