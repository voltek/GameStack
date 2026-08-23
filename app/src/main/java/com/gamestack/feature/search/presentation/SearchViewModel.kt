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

    // Effective query [searchJob] serves, or null when nothing is running. Two
    // opposite situations otherwise reach the same branch below: a job already
    // searching this exact text must be left to land, while a job scheduled for
    // text the user has since abandoned must be cancelled. Only the query the
    // job was started for tells them apart.
    private var searchJobQuery: String? = null

    // Trimmed value of the query whose results are currently on screen — set on
    // success only (see performSearch). Lets onQueryChanged tell "user
    // added/removed leading/trailing whitespace" apart from "the effective
    // search text changed", without altering what the text field shows.
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
            cancelSearch()
            lastSearchedQuery = ""
            _uiState.update {
                it.copy(
                    games = emptyList(),
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null
                )
            }
            return
        }

        // A job is already serving this exact effective query — the user only
        // added or removed surrounding whitespace while it ran. Cancelling here
        // would throw away the response that is about to arrive.
        if (trimmedQuery == searchJobQuery) return

        // Anything still scheduled is for text the user has since changed, so it
        // must never land — including on the path below that starts no new
        // search of its own.
        cancelSearch()

        // Same effective query as the last one actually searched — nothing new to
        // search for, and the results already on screen are the right ones.
        if (trimmedQuery == lastSearchedQuery) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        startSearch(trimmedQuery, isRefresh = false)
    }

    private fun onRefresh() {
        val trimmedQuery = _uiState.value.query.trim()
        if (trimmedQuery.isEmpty()) return

        cancelSearch()
        startSearch(trimmedQuery, isRefresh = true)
    }

    private fun cancelSearch() {
        searchJob?.cancel()
        searchJob = null
        searchJobQuery = null
    }

    private fun startSearch(query: String, isRefresh: Boolean) {
        searchJobQuery = query
        searchJob = viewModelScope.launch {
            // A refresh is an explicit user action, so it runs immediately; a
            // keystroke waits out the debounce.
            if (!isRefresh) delay(SearchDebounceMillis)
            performSearch(query, isRefresh)
            searchJobQuery = null
        }
    }

    private suspend fun performSearch(query: String, isRefresh: Boolean) {
        _uiState.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh, errorMessage = null) }

        searchGamesUseCase(query)
            .onSuccess { games ->
                // Recorded only now, not at dispatch: it answers "which query do
                // the results on screen belong to". A request that was cancelled
                // or failed never produced results, so claiming it as searched
                // would let a later identical query skip the search entirely and
                // leave stale or empty content on screen.
                lastSearchedQuery = query
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
