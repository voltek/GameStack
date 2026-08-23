package com.gamestack.feature.search.presentation

import app.cash.turbine.test
import com.gamestack.core.domain.model.Game
import com.gamestack.core.testing.MainDispatcherRule
import com.gamestack.feature.search.domain.usecase.SearchGamesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    // Own dispatcher instance (not the rule's default) so its scheduler can be driven
    // directly — that's what actually resolves the ViewModel's debounce delay() below.
    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val searchGamesUseCase: SearchGamesUseCase = mockk()
    private val viewModel by lazy { SearchViewModel(searchGamesUseCase) }

    private val sampleGame = Game(
        id = 1,
        name = "Elden Ring",
        coverUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1.jpg",
        genres = listOf("RPG"),
        developer = "FromSoftware"
    )

    private fun advanceDebounce() {
        testDispatcher.scheduler.advanceUntilIdle()
    }

    // Regression: reverting to the previously searched text must also cancel the
    // job scheduled for the intermediate query, or that job lands later and paints
    // results for text the field no longer shows.
    @Test
    fun `handleEvent OnQueryChanged should cancel a pending search when the query reverts to the last searched one`() =
        runTest {
            coEvery { searchGamesUseCase(any()) } returns Result.success(listOf(sampleGame))

            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("zel"))
            advanceDebounce()
            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("zeld"))
            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("zel"))
            advanceDebounce()

            coVerify(exactly = 0) { searchGamesUseCase("zeld") }
            coVerify(exactly = 1) { searchGamesUseCase("zel") }
            assertEquals("zel", viewModel.uiState.value.query)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    // Regression: cancelling an in-flight refresh by clearing the query left
    // isRefreshing stuck true, so the pull-to-refresh indicator spun forever.
    @Test
    fun `handleEvent OnClearQuery should reset isRefreshing when a refresh is in flight`() =
        runTest {
            coEvery { searchGamesUseCase("Elden Ring") } returns Result.success(listOf(sampleGame))
            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("Elden Ring"))
            advanceDebounce()

            viewModel.handleEvent(SearchUiEvent.OnRefresh)
            viewModel.handleEvent(SearchUiEvent.OnClearQuery)
            advanceDebounce()

            val state = viewModel.uiState.value
            assertFalse(state.isRefreshing)
            assertFalse(state.isLoading)
            assertTrue(state.games.isEmpty())
        }

    // Regression: the error card stayed on screen for the whole debounce window
    // of the next keystroke, because errorMessage outlived the failed search.
    @Test
    fun `handleEvent OnQueryChanged should clear errorMessage as soon as a new query starts debouncing`() =
        runTest {
            coEvery { searchGamesUseCase("Elden") } returns Result.failure(RuntimeException("network down"))
            coEvery { searchGamesUseCase("Elden Ring") } returns Result.success(listOf(sampleGame))
            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("Elden"))
            advanceDebounce()
            assertNotNull(viewModel.uiState.value.errorMessage)

            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("Elden Ring"))

            val state = viewModel.uiState.value
            assertNull(state.errorMessage)
            assertTrue(state.isLoading)
        }

    // Spec: query text updates immediately, independent of the debounced search.
    @Test
    fun `handleEvent OnQueryChanged should update query immediately`() = runTest {
        coEvery { searchGamesUseCase(any()) } returns Result.success(emptyList())

        viewModel.handleEvent(SearchUiEvent.OnQueryChanged("Elden"))

        assertEquals("Elden", viewModel.uiState.value.query)
    }

    // Spec: after the debounce window, the search runs and results populate state.
    @Test
    fun `handleEvent OnQueryChanged should populate games after debounce when search succeeds`() =
        runTest {
            coEvery { searchGamesUseCase("Elden Ring") } returns Result.success(listOf(sampleGame))

            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("Elden Ring"))
            advanceDebounce()

            val state = viewModel.uiState.value
            assertEquals(listOf(sampleGame), state.games)
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
        }

    // Spec: isLoading must cover the whole debounce window, not just the network
    // call — otherwise the empty-results state briefly shows before any search runs.
    @Test
    fun `handleEvent OnQueryChanged should set isLoading immediately, before the debounce completes`() =
        runTest {
            coEvery { searchGamesUseCase(any()) } returns Result.success(emptyList())

            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("Elden"))

            val state = viewModel.uiState.value
            assertTrue(state.isLoading)
            assertTrue(state.games.isEmpty())
            coVerify(exactly = 0) { searchGamesUseCase(any()) }
        }

    // Spec: adding/removing only whitespace around an already-searched query must
    // not trigger another debounced search — the effective query hasn't changed.
    @Test
    fun `handleEvent OnQueryChanged should not repeat a search when only whitespace changes`() =
        runTest {
            coEvery { searchGamesUseCase("Marvel") } returns Result.success(listOf(sampleGame))

            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("Marvel"))
            advanceDebounce()
            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("  Marvel"))
            advanceDebounce()
            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("  Marvel  "))
            advanceDebounce()

            coVerify(exactly = 1) { searchGamesUseCase("Marvel") }
        }

    // Spec: the text field must keep showing whitespace exactly as typed, even
    // though it's not sent to search.
    @Test
    fun `handleEvent OnQueryChanged should keep raw whitespace in query state`() = runTest {
        coEvery { searchGamesUseCase(any()) } returns Result.success(emptyList())

        viewModel.handleEvent(SearchUiEvent.OnQueryChanged("  Marvel"))

        assertEquals("  Marvel", viewModel.uiState.value.query)
    }

    @Test
    fun `handleEvent OnQueryChanged should cancel previous search when a new query arrives before debounce completes`() =
        runTest {
            coEvery { searchGamesUseCase(any()) } returns Result.success(listOf(sampleGame))

            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("Eld"))
            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("Elden Ring"))
            advanceDebounce()

            coVerify(exactly = 0) { searchGamesUseCase("Eld") }
            coVerify(exactly = 1) { searchGamesUseCase("Elden Ring") }
        }

    @Test
    fun `handleEvent OnRefresh should cancel pending debounced search`() = runTest {
        coEvery { searchGamesUseCase(any()) } returns Result.success(listOf(sampleGame))

        viewModel.handleEvent(SearchUiEvent.OnQueryChanged("Eld"))
        viewModel.handleEvent(SearchUiEvent.OnQueryChanged("Elden Ring")) // still debouncing
        viewModel.handleEvent(SearchUiEvent.OnRefresh)
        advanceDebounce()

        coVerify(exactly = 0) { searchGamesUseCase("Eld") }
        coVerify(exactly = 1) { searchGamesUseCase("Elden Ring") }
    }

    // Spec: a use case failure surfaces as errorMessage, not a crash.
    @Test
    fun `handleEvent OnQueryChanged should set errorMessage when search fails`() = runTest {
        coEvery { searchGamesUseCase("Elden Ring") } returns Result.failure(RuntimeException("network down"))

        viewModel.handleEvent(SearchUiEvent.OnQueryChanged("Elden Ring"))
        advanceDebounce()

        val state = viewModel.uiState.value
        assertTrue(state.games.isEmpty())
        assertNotNull(state.errorMessage)
        assertFalse(state.isLoading)
    }

    // Spec: clearing the query resets results without waiting on any debounce.
    @Test
    fun `handleEvent OnClearQuery should reset query and games`() = runTest {
        coEvery { searchGamesUseCase("Elden Ring") } returns Result.success(listOf(sampleGame))
        viewModel.handleEvent(SearchUiEvent.OnQueryChanged("Elden Ring"))
        advanceDebounce()

        viewModel.handleEvent(SearchUiEvent.OnClearQuery)

        val state = viewModel.uiState.value
        assertEquals("", state.query)
        assertTrue(state.games.isEmpty())
    }

    // Spec: refresh repeats the last query (pull-to-refresh pattern).
    @Test
    fun `handleEvent OnRefresh should repeat the last query`() = runTest {
        coEvery { searchGamesUseCase("Elden Ring") } returns Result.success(listOf(sampleGame))
        viewModel.handleEvent(SearchUiEvent.OnQueryChanged("Elden Ring"))
        advanceDebounce()

        viewModel.handleEvent(SearchUiEvent.OnRefresh)
        advanceDebounce()

        coVerify(exactly = 2) { searchGamesUseCase("Elden Ring") }
        assertEquals(listOf(sampleGame), viewModel.uiState.value.games)
    }

    // Spec: tapping a result emits a one-shot navigation effect carrying the game id.
    @Test
    fun `handleEvent OnGameClicked should emit NavigateToGameDetail effect`() = runTest {
        viewModel.uiEffect.test {
            viewModel.handleEvent(SearchUiEvent.OnGameClicked(42))

            val effect = awaitItem()
            assertTrue(effect is SearchUiEffect.NavigateToGameDetail)
            assertEquals(42, (effect as SearchUiEffect.NavigateToGameDetail).gameId)
        }
    }

    // Spec: the empty-state "Go To Library" action emits a one-shot navigation effect.
    @Test
    fun `handleEvent OnGoToLibraryClicked should emit NavigateToLibrary effect`() = runTest {
        viewModel.uiEffect.test {
            viewModel.handleEvent(SearchUiEvent.OnGoToLibraryClicked)

            val effect = awaitItem()
            assertTrue(effect is SearchUiEffect.NavigateToLibrary)
        }
    }

    // Regression: cancelling unconditionally on every keystroke also killed the
    // request already running for the same effective query. On a first search
    // there were no results to fall back on, so the screen dropped straight to
    // "No Results Found" and the response that was about to arrive was discarded.
    @Test
    fun `handleEvent OnQueryChanged should keep an in-flight search alive when only whitespace changes`() =
        runTest {
            coEvery { searchGamesUseCase("zelda") } coAnswers {
                delay(IN_FLIGHT_MILLIS)
                Result.success(listOf(sampleGame))
            }

            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("zelda"))
            // Past the debounce: the request is now running, not merely scheduled.
            testDispatcher.scheduler.advanceTimeBy(PAST_DEBOUNCE_MILLIS)

            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("zelda "))

            // The bug showed here: isLoading went false with games still empty.
            assertTrue(viewModel.uiState.value.isLoading)
            assertTrue(viewModel.uiState.value.games.isEmpty())

            advanceDebounce()

            val state = viewModel.uiState.value
            assertEquals(listOf(sampleGame), state.games)
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
            coVerify(exactly = 1) { searchGamesUseCase("zelda") }
        }

    // Refreshes are dispatched immediately, so the same guard must not let a
    // whitespace keystroke cancel one mid-flight either.
    @Test
    fun `handleEvent OnQueryChanged should keep an in-flight refresh alive when only whitespace changes`() =
        runTest {
            coEvery { searchGamesUseCase("zelda") } returns Result.success(emptyList())
            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("zelda"))
            advanceDebounce()

            coEvery { searchGamesUseCase("zelda") } coAnswers {
                delay(IN_FLIGHT_MILLIS)
                Result.success(listOf(sampleGame))
            }
            viewModel.handleEvent(SearchUiEvent.OnRefresh)
            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("zelda "))

            assertTrue(viewModel.uiState.value.isRefreshing)

            advanceDebounce()

            assertEquals(listOf(sampleGame), viewModel.uiState.value.games)
            assertFalse(viewModel.uiState.value.isRefreshing)
        }

    // Regression: marking a query as searched at dispatch time made that flag lie
    // whenever the request was later cancelled. Typing a character and deleting it
    // while the first search was still in flight left the restored query matching
    // the flag, so no search restarted and the screen kept the empty state.
    @Test
    fun `handleEvent OnQueryChanged should search again when the first request was cancelled before returning`() =
        runTest {
            coEvery { searchGamesUseCase("zelda") } coAnswers {
                delay(IN_FLIGHT_MILLIS)
                Result.success(listOf(sampleGame))
            }
            coEvery { searchGamesUseCase("zeldax") } returns Result.success(emptyList())

            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("zelda"))
            // Past the debounce: "zelda" has reached the UseCase but has not returned.
            testDispatcher.scheduler.advanceTimeBy(PAST_DEBOUNCE_MILLIS)

            // A different character cancels that request, then is deleted again.
            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("zeldax"))
            viewModel.handleEvent(SearchUiEvent.OnQueryChanged("zelda"))
            advanceDebounce()

            assertEquals(listOf(sampleGame), viewModel.uiState.value.games)
            assertFalse(viewModel.uiState.value.isLoading)
            coVerify(exactly = 2) { searchGamesUseCase("zelda") }
        }

    private companion object {
        // Longer than the ViewModel's debounce, which is private to its file.
        const val PAST_DEBOUNCE_MILLIS = 500L
        const val IN_FLIGHT_MILLIS = 1_000L
    }
}
