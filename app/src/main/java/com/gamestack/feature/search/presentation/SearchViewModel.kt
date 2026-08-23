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

    // Two operators replace what used to be hand-written bookkeeping across
    // several nullable fields, and with it a whole class of defects:
    //  - distinctUntilChanged after trim: a whitespace-only edit is not a new
    //    query, so it neither restarts a search nor disturbs one in flight.
    //  - mapLatest: a newer request cancels whatever the previous one was doing,
    //    whether that was waiting out its debounce or already awaiting a
    //    response.
    // Nothing here records "which query was already searched"; every earlier
    // version of this class did, and every one of them eventually disagreed with
    // what was actually on screen.
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
            // The wait lives here rather than in a debounce() operator upstream.
            // An operator would hold the new request for its own timeout while
            // the previous search kept running, so a response for text the user
            // had already changed could still land — showing settled results, or
            // flashing the error card, for the whole debounce window. Inside
            // mapLatest the new request cancels the running one first, then waits.
            delay(request.debounceMillis)
            // An empty query still travels the pipeline: reaching this point is
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
                    errorMessage = null,
                    refreshError = null
                )

                startsSearch -> state.copy(
                    query = query,
                    // games must never outlive the query it answers: anything
                    // still holding them can only misread them as current.
                    games = emptyList(),
                    // Optimistic, so the skeleton appears while the debounce runs
                    // rather than 400ms into the wait.
                    isLoading = true,
                    // Typing supersedes a refresh, and mapLatest cancels it, so
                    // its coroutine will never reset this flag itself.
                    isRefreshing = false,
                    errorMessage = null,
                    // The stale results it described are gone, so is it.
                    refreshError = null
                )

                else -> state.copy(query = query)
            }
        }

        typedQuery.value = query
    }

    private fun onRefresh() {
        if (_uiState.value.query.isBlank()) return
        // Repeat taps on the banner's Retry would each reach IGDB, whose quota is
        // finite. mapLatest and the buffered SharedFlow already keep the *state*
        // consistent, so this exists purely to stop redundant requests.
        if (_uiState.value.isRefreshing) return
        // Set here, not in performSearch: PullToRefreshBox reads this flag
        // synchronously when the pull is released and retracts the indicator if
        // it is still false, so leaving it to a coroutine hop makes the spinner
        // visibly bounce back and re-appear on every pull.
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
                        // Fresh results resolve the condition the banner reports,
                        // so it goes with them. Nothing else has to remember to
                        // take it down.
                        refreshError = null
                    )
                }
            }
            .onFailure {
                // Results on screen always belong to the current query (they are
                // cleared the moment it changes), so having any means this was a
                // refresh of an answered query: keep them and report the failure
                // alongside rather than losing them to a blip. Nothing on screen
                // means the error state is all there is to show.
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
