package com.gamestack.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamestack.R
import com.gamestack.core.presentation.UiText
import com.gamestack.feature.search.domain.usecase.SearchGamesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

// Both exceptions to the debounce are properties of the request: a refresh is an
// explicit action, and an emptied field must clear the screen at once.
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

    // Raw text as typed: the pipeline trims it, the field shows what was written.
    private val typedQuery = MutableStateFlow("")
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // No field records which query was already searched — the operators carry it.
    // distinctUntilChanged after trim: a whitespace-only edit is not a new query,
    // so it neither restarts a search nor disturbs one in flight. See the
    // debounced-pipeline pattern in the new-viewmodel Skill.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchPipeline = merge(
        typedQuery
            .map { it.trim() }
            .distinctUntilChanged()
            .map { SearchRequest(query = it, isRefresh = false) },
        refreshRequests
            .map { SearchRequest(query = typedQuery.value.trim(), isRefresh = true) }
    )
        .mapLatest { request ->
            // Must stay inside mapLatest, never a debounce() operator upstream:
            // upstream, the new request waits out its own timeout while the
            // previous search keeps running, so a response for replaced text can
            // still land. Here the newer request cancels first, then waits.
            delay(request.debounceMillis)
            // An empty query still travels the pipeline — reaching here is what
            // cancels a search running for the text just deleted.
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
        // Mirrors the distinctUntilChanged above, so a whitespace-only edit leaves
        // every transient flag exactly as it is.
        val startsSearch = trimmedQuery != typedQuery.value.trim()

        _uiState.update { state ->
            when {
                trimmedQuery.isEmpty() -> state.copy(
                    query = query,
                    games = emptyList(),
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null,
                    refreshError = null
                )

                startsSearch -> state.copy(
                    query = query,
                    // Results must never outlive the query they answer.
                    games = emptyList(),
                    // Optimistic, so the skeleton appears during the debounce.
                    isLoading = true,
                    // mapLatest cancels the superseded refresh, so its coroutine
                    // will never reset this flag itself.
                    isRefreshing = false,
                    errorMessage = null,
                    refreshError = null
                )

                else -> state.copy(query = query)
            }
        }

        typedQuery.value = query
    }

    private fun onRefresh() {
        if (_uiState.value.query.isBlank()) return
        // State stays consistent without this (mapLatest cancels the predecessor);
        // it exists only to stop repeat Retry taps burning IGDB's finite quota.
        if (_uiState.value.isRefreshing) return
        // Must be synchronous: PullToRefreshBox reads this flag when the pull is
        // released and retracts the indicator if it is still false, so a
        // coroutine hop makes the spinner bounce back on every pull.
        _uiState.update { it.copy(isRefreshing = true) }
        refreshRequests.tryEmit(Unit)
    }

    private suspend fun performSearch(query: String, isRefresh: Boolean) {
        _uiState.update { it.copy(isLoading = !isRefresh, isRefreshing = isRefresh, errorMessage = null) }

        searchGamesUseCase(query)
            .onSuccess { games ->
                _uiState.update {
                    it.copy(
                        games = games,
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null,
                        // Fresh results resolve the condition the banner reports.
                        refreshError = null
                    )
                }
            }
            .onFailure {
                // Results on screen always belong to the current query, so having
                // any means this was a refresh of an already-answered one: keep
                // them rather than lose them to a blip. This is why no isRefresh
                // flag is needed here.
                val keepResults = _uiState.value.games.isNotEmpty()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = if (keepResults) {
                            null
                        } else {
                            UiText.StringResource(R.string.search_error_description)
                        },
                        refreshError = if (keepResults) {
                            UiText.StringResource(R.string.search_refresh_error)
                        } else {
                            null
                        }
                    )
                }
            }
    }

    private fun sendEffect(effect: SearchUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
