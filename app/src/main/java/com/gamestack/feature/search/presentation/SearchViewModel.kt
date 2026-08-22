package com.gamestack.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamestack.R
import com.gamestack.core.presentation.UiText
import com.gamestack.feature.search.domain.usecase.SearchGamesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SearchDebounceMillis = 400L

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchGamesUseCase: SearchGamesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<SearchUiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    private var searchJob: Job? = null

    // Trimmed value of the last query actually dispatched to the UseCase (via
    // debounce or refresh) — lets onQueryChanged tell "user added/removed
    // leading/trailing whitespace" apart from "the effective search text
    // changed", without altering what's shown in the text field itself.
    private var lastSearchedQuery: String = ""

    fun handleEvent(event: SearchUiEvent) {
        when (event) {
            is SearchUiEvent.OnQueryChanged -> onQueryChanged(event.query)
            SearchUiEvent.OnClearQuery -> onQueryChanged("")
            SearchUiEvent.OnRefresh -> onRefresh()
            is SearchUiEvent.OnGameClicked -> sendEffect(SearchUiEffect.NavigateToGameDetail(event.gameId))
            SearchUiEvent.OnGoToLibraryClicked -> sendEffect(SearchUiEffect.NavigateToLibrary)
        }
    }

    private fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }

        val trimmedQuery = query.trim()

        if (trimmedQuery.isEmpty()) {
            searchJob?.cancel()
            lastSearchedQuery = ""
            _uiState.update { it.copy(games = emptyList(), isLoading = false, errorMessage = null) }
            return
        }

        // Same effective query as the last one actually searched (e.g. only
        // whitespace was added/removed) — nothing new to search for.
        if (trimmedQuery == lastSearchedQuery) return

        searchJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        searchJob = viewModelScope.launch {
            delay(SearchDebounceMillis)
            lastSearchedQuery = trimmedQuery
            performSearch(trimmedQuery, isRefresh = false)
        }
    }

    private fun onRefresh() {
        val trimmedQuery = _uiState.value.query.trim()
        if (trimmedQuery.isEmpty()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            lastSearchedQuery = trimmedQuery
            performSearch(trimmedQuery, isRefresh = true)
        }
    }

    private suspend fun performSearch(query: String, isRefresh: Boolean) {
        _uiState.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh, errorMessage = null) }

        searchGamesUseCase(query)
            .onSuccess { games ->
                _uiState.update {
                    it.copy(games = games, isLoading = false, isRefreshing = false, errorMessage = null)
                }
            }
            .onFailure {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = UiText.StringResource(R.string.search_error_description)
                    )
                }
            }
    }

    private fun sendEffect(effect: SearchUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
