package com.example.sampleapp

class PlaylistViewModel(
    private val fetchPlaylistUseCase: FetchPlaylistUseCase,
    private val repository: PlaylistRepository,
) {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val channels: List<Channel>, val category: String) : UiState()
        data class Empty(val category: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    private var _uiState: UiState = UiState.Loading
    val uiState: UiState get() = _uiState

    private var currentCategory: String = ""

    fun loadCategory(category: String) {
        _uiState = UiState.Loading
        currentCategory = category
        _uiState = when (val result = fetchPlaylistUseCase.execute(category)) {
            is FetchPlaylistUseCase.Result.Success -> UiState.Success(result.channels, category)
            is FetchPlaylistUseCase.Result.Empty -> UiState.Empty(category)
            is FetchPlaylistUseCase.Result.Error -> UiState.Error(result.message)
        }
    }

    fun search(query: String): List<Channel> {
        val result = fetchPlaylistUseCase.search(query)
        return if (result is FetchPlaylistUseCase.Result.Success) result.channels else emptyList()
    }

    fun deleteCategory(category: String): Boolean {
        val deleted = repository.deleteCategory(category)
        if (deleted && category == currentCategory) {
            _uiState = UiState.Empty(category)
        }
        return deleted
    }

    fun refresh() {
        if (currentCategory.isNotBlank()) {
            loadCategory(currentCategory)
        }
    }
}
