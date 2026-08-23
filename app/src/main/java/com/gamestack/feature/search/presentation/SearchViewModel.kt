package com.gamestack.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamestack.R
import com.gamestack.core.presentation.UiText
import com.gamestack.feature.search.domain.usecase.SearchGamesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SearchDebounceMillis = 400L

// What the pipeline carries: an effective (trimmed) query plus how it was asked
// for. Both exceptions to the debounce are properties of the request itself — a
// refresh is an explicit user action, and an emptied field must clear the screen
// at once rather than 400ms later.
private data class SearchRequest(val query: String, val isRefresh: Boolean) {
    val debounceMillis: Long get() = if (isRefresh || query.isEmpty()) 0L else SearchDebounceMillis
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchGamesUseCase: SearchGamesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<SearchUiEffect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    // Raw text as typed, whitespace included — the pipeline trims it, the text
    // field keeps showing exactly what the user wrote.
    private val typedQuery = MutableStateFlow("")
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Three operators replace what used to be hand-written bookkeeping across
    // several nullable fields, and with it a whole class of defects:
    //  - distinctUntilChanged after trim: a whitespace-only edit is not a new
    //    query, so it neither restarts a search nor disturbs one in flight.
    //  - debounce: drops whatever was still pending when a newer request
    //    arrives, so a refresh supersedes a keystroke that never got dispatched.
    //  - mapLatest: cancels the search already running when a newer one starts,
    //    so no stale response can land after the query moved on.
    // Nothing here records "which query was already searched"; every earlier
    // version of this class did, and every one of them eventually disagreed with
    // what was actually on screen.
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val searchPipeline = merge(
        typedQuery
            .map { it.trim() }
            .distinctUntilChanged()
            .map { SearchRequest(query = it, isRefresh = false) },
        refreshRequests
            .map { SearchRequest(query = typedQuery.value.trim(), isRefresh = true) }
    )
        .debounce { it.debounceMillis }
        .mapLatest { request ->
            // An empty query still travels the pipeline: reaching mapLatest is
            // what cancels any search still running for the text just deleted.
            if (request.query.isNotEmpty()) performSearch(request.query, request.isRefresh)
        }
        .launchIn(viewModelScope)

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
        val trimmedQuery = query.trim()
        // Mirrors the distinctUntilChanged above: only a changed effective query
        // will reach the network, and only then is the screen out of date. A
        // whitespace-only edit must leave every transient flag exactly as it is.
        val startsSearch = trimmedQuery != typedQuery.value.trim()

        _uiState.update { state ->
            when {
                trimmedQuery.isEmpty() -> state.copy(
                    query = query,
                    games = emptyList(),
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null
                )

                startsSearch -> state.copy(
                    query = query,
                    // Optimistic, so the skeleton appears while the debounce runs
                    // rather than 400ms into the wait.
                    isLoading = true,
                    // Cleared here so a failure from the query being replaced
                    // cannot outlive it and hide the results that do arrive.
                    errorMessage = null
                )

                else -> state.copy(query = query)
            }
        }

        typedQuery.value = query
    }

    private fun onRefresh() {
        if (_uiState.value.query.isBlank()) return
        refreshRequests.tryEmit(Unit)
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
