package com.gamestack.feature.search.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.gamestack.R
import com.gamestack.core.domain.model.Game
import com.gamestack.core.presentation.UiText
import com.gamestack.core.presentation.rememberSingleClick
import com.gamestack.core.presentation.components.MessageState
import com.gamestack.core.presentation.components.ShimmerPlaceholder
import com.gamestack.ui.theme.GameStackTheme
import kotlinx.coroutines.flow.collectLatest

private const val GameCoverAspectRatio = 3f / 4f

@Composable
fun SearchScreen(
    onNavigateToGameDetail: (Int) -> Unit,
    onNavigateToLibrary: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val retryLabel = stringResource(R.string.search_error_action)

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is SearchUiEffect.NavigateToGameDetail -> onNavigateToGameDetail(effect.gameId)
                SearchUiEffect.NavigateToLibrary -> onNavigateToLibrary()
                is SearchUiEffect.ShowRefreshError -> {
                    val result = snackbarHostState.showSnackbar(
                        message = effect.message.asString(context),
                        actionLabel = retryLabel,
                        withDismissAction = true,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.handleEvent(SearchUiEvent.OnRefresh)
                    }
                }
            }
        }
    }

    // The snackbar announces one failed refresh of one query. Once the query
    // changes its message is about something the user has moved on from, and its
    // Retry would act on the new text instead — or do nothing at all if the field
    // was cleared, since a blank query cannot be refreshed.
    LaunchedEffect(uiState.query) {
        snackbarHostState.currentSnackbarData?.dismiss()
    }

    // The host lives here rather than in GameStackApp's Scaffold: Search is the
    // only screen that raises a snackbar today, and NavHost content is already
    // inset by that Scaffold's padding, so bottom-aligning it here clears the
    // bottom nav with no offset maths. Promote it to the app Scaffold as soon as
    // a second screen needs one — two local hosts would be the wrong shape.
    Box(modifier = Modifier.fillMaxSize()) {
        SearchContent(uiState = uiState, onEvent = viewModel::handleEvent)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SearchContent(
    uiState: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    // One wrapper for the whole grid, so a repeat tap on the same card *and* two
    // quick taps on different cards both emit a single navigation effect.
    val onGameClicked = rememberSingleClick<Int> { onEvent(SearchUiEvent.OnGameClicked(it)) }

    // Every callback that emits a navigation effect needs the guard, not just the
    // ones in lists — this CTA was missed when the wrapper was introduced.
    val onGoToLibrary = rememberSingleClick { onEvent(SearchUiEvent.OnGoToLibraryClicked) }

    Column(modifier = modifier.fillMaxSize()) {
        SearchBar(
            query = uiState.query,
            onQueryChanged = { onEvent(SearchUiEvent.OnQueryChanged(it)) },
            onClear = { onEvent(SearchUiEvent.OnClearQuery) },
            onSearchAction = dismissKeyboard,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { onEvent(SearchUiEvent.OnRefresh) },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // Initial pass fires before any child (card click,
                        // grid scroll) gets a chance to consume the gesture —
                        // detectTapGestures here would miss drags entirely,
                        // since a child consuming touch-slop cancels it.
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        dismissKeyboard()
                    }
                }
        ) {
            // imePadding only on the message states: they are centred and
            // non-scrollable, so their CTA would sit behind the keyboard. The
            // grids keep full height — shrinking them just squeezes results into
            // fewer rows, and scrolling them dismisses the keyboard anyway.
            when {
                uiState.errorMessage != null -> MessageState(
                    icon = Icons.Filled.ErrorOutline,
                    title = stringResource(R.string.search_error_title),
                    description = uiState.errorMessage.asString(),
                    modifier = Modifier.imePadding(),
                    actionLabel = stringResource(R.string.search_error_action),
                    actionIcon = Icons.Filled.Refresh,
                    onAction = { onEvent(SearchUiEvent.OnRefresh) }
                )

                // A refresh with nothing to keep on screen (e.g. Retry after a
                // failed first search) shows the skeleton too — otherwise it
                // falls through to "No Results Found" for the whole request,
                // which reads as an answer rather than as work in progress.
                uiState.isLoading || (uiState.isRefreshing && uiState.games.isEmpty()) ->
                    SearchLoadingGrid()

                uiState.query.isBlank() -> MessageState(
                    icon = Icons.Filled.Search,
                    title = stringResource(R.string.search_initial_title),
                    description = stringResource(R.string.search_initial_description),
                    modifier = Modifier.imePadding()
                )

                uiState.games.isEmpty() -> MessageState(
                    icon = Icons.Filled.SearchOff,
                    title = stringResource(R.string.search_empty_title),
                    description = stringResource(R.string.search_empty_description),
                    modifier = Modifier.imePadding(),
                    actionLabel = stringResource(R.string.search_empty_action),
                    onAction = onGoToLibrary
                )

                else -> SearchResultsGrid(games = uiState.games, onGameClicked = onGameClicked)
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClear: () -> Unit,
    onSearchAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier,
        placeholder = { Text(stringResource(R.string.search_bar_placeholder)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.search_bar_clear_content_description)
                    )
                }
            }
        },
        singleLine = true,
        // Results already arrive on their own via the debounce, so the IME's
        // Search key doesn't trigger a search — it means "I'm done typing":
        // it dismisses the keyboard so the results are fully visible.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun SearchResultsGrid(
    games: List<Game>,
    onGameClicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(games, key = { it.id }) { game ->
            GameCard(game = game, onClick = { onGameClicked(game.id) })
        }
    }
}

@Composable
private fun GameCard(game: Game, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(GameCoverAspectRatio)
        ) {
            AsyncImage(
                model = game.coverUrl,
                contentDescription = game.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentScale = ContentScale.Crop
            )
            val primaryGenre = game.genres.firstOrNull()
            if (primaryGenre != null) {
                Text(
                    text = primaryGenre.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            Color.DarkGray.copy(alpha = 0.6f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Text(
            text = game.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (game.developer != null) {
            Text(
                text = game.developer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private const val LoadingSkeletonItemCount = 6

@Composable
private fun SearchLoadingGrid(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(LoadingSkeletonItemCount) {
            ShimmerPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(GameCoverAspectRatio)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchContentLoadingPreview() {
    GameStackTheme {
        SearchContent(uiState = SearchUiState(query = "Elden Ring", isLoading = true), onEvent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchContentResultsPreview() {
    GameStackTheme {
        SearchContent(
            uiState = SearchUiState(
                query = "Elden Ring",
                games = listOf(
                    Game(1, "Elden Ring", null, listOf("RPG"), "FromSoftware"),
                    Game(2, "God of War Ragnarok", null, listOf("Action"), "Santa Monica Studio")
                )
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchContentEmptyPreview() {
    GameStackTheme {
        SearchContent(uiState = SearchUiState(query = "asdkjhaksjd"), onEvent = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchContentErrorPreview() {
    GameStackTheme {
        SearchContent(
            uiState = SearchUiState(
                query = "Elden Ring",
                errorMessage = UiText.DynamicString(
                    "We're having trouble loading your games. Please check your connection and try again."
                )
            ),
            onEvent = {}
        )
    }
}
