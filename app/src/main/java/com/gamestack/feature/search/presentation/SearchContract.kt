package com.gamestack.feature.search.presentation

import com.gamestack.core.domain.model.Game
import com.gamestack.core.presentation.UiText

data class SearchUiState(
    val query: String = "",
    val games: List<Game> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: UiText? = null
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

    // A refresh that fails while results are still on screen. It is an effect and
    // not a UiState field because the results stay: the failure is announced over
    // them and then gone, which is a one-shot event, not a state the screen is in.
    // [query] is the effective query it belongs to — the Channel can hold it
    // while the user types, and a message about text they have left is worse than
    // no message, so the screen drops it rather than showing it late.
    data class ShowRefreshError(val message: UiText, val query: String) : SearchUiEffect()
}
