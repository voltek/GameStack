package com.gamestack.feature.search.presentation

import com.gamestack.core.domain.model.Game
import com.gamestack.core.presentation.UiText

data class SearchUiState(
    val query: String = "",
    val games: List<Game> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: UiText? = null,
    // A refresh failed while [games] stayed on screen. State rather than a
    // one-shot effect because it describes a condition that lasts until
    // something resolves it, not a moment: it clears itself when a load
    // succeeds or the query changes. Announcing it transiently instead cost
    // three separate defects, each a different way for the announcement's
    // lifetime to drift from the condition's.
    val refreshError: UiText? = null
)

sealed class SearchUiEvent {
    data class OnQueryChanged(val query: String) : SearchUiEvent()
    data object OnClearQuery : SearchUiEvent()
    data object OnRefresh : SearchUiEvent()
    data class OnGameClicked(val gameId: Int) : SearchUiEvent()
    data object OnGoToLibraryClicked : SearchUiEvent()
}

sealed class SearchUiEffect {
    data class NavigateToGameDetail(val gameId: Int) : SearchUiEffect()
    data object NavigateToLibrary : SearchUiEffect()
}
