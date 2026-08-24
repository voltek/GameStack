package com.gamestack.feature.search.presentation

import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gamestack.R
import com.gamestack.core.domain.model.Game
import com.gamestack.core.presentation.SingleClickWindowMillis
import com.gamestack.core.presentation.UiText
import com.gamestack.ui.theme.GameStackTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

// No @Config here: the SDK is pinned project-wide in test/resources/robolectric
// .properties, so a class that forgets one cannot silently run on another API.
@RunWith(RobolectricTestRunner::class)
class SearchScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val games = listOf(
        Game(1, "Elden Ring", null, listOf("RPG"), "FromSoftware"),
        Game(2, "Hollow Knight", null, listOf("Metroidvania"), "Team Cherry")
    )

    private fun setContent(state: SearchUiState, onEvent: (SearchUiEvent) -> Unit = {}) {
        composeRule.setContent { GameStackTheme { SearchContent(uiState = state, onEvent = onEvent) } }
    }

    // Bound to strings.xml rather than copied from it: the initial-state title and
    // the search bar's placeholder differ by a single trailing ellipsis, so a
    // duplicated literal turns a copy tweak into a node-count failure naming
    // nothing.
    private fun string(@StringRes id: Int): String =
        RuntimeEnvironment.getApplication().getString(id)

    private fun assertAbsent(text: String) = assertTrue(
        "\"$text\" must not be on screen",
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
    )

    // Spec: a failed refresh keeps the results on screen and reports the failure
    // above them. Ordering is asserted, not just co-existence: the grid is
    // fillMaxSize, so a banner placed after it would be pushed off screen while
    // both nodes still "exist".
    @Test
    fun `refresh error state shows the banner above the results, not instead of them`() {
        setContent(
            SearchUiState(
                query = "Elden",
                games = games,
                refreshError = UiText.StringResource(R.string.search_refresh_error)
            )
        )

        val banner = composeRule.onNodeWithText(string(R.string.search_refresh_error))
        val firstResult = composeRule.onNodeWithText("Elden Ring")
        banner.assertIsDisplayed()
        firstResult.assertIsDisplayed()

        assertTrue(
            "The banner must sit above the results",
            banner.getBoundsInRoot().top < firstResult.getBoundsInRoot().top
        )
    }

    // Spec: the banner announces itself. A Snackbar did so for free and a Row does
    // not, so the live region is what keeps a screen-reader user from reading a
    // stale list with no cue. This asserts the declaration, which is all a test can
    // reach — whether TalkBack speaks it on appearance needs a manual pass.
    @Test
    fun `refresh error banner declares a live region so it is announced`() {
        setContent(
            SearchUiState(
                query = "Elden",
                games = games,
                refreshError = UiText.StringResource(R.string.search_refresh_error)
            )
        )

        val config = composeRule.onNodeWithText(string(R.string.search_refresh_error))
            .fetchSemanticsNode()
            .config

        assertTrue(
            "The banner must declare a live region",
            config.contains(SemanticsProperties.LiveRegion)
        )
    }

    // Spec: a refresh already in flight makes Retry a no-op, so the control reports
    // itself unavailable rather than looking actionable.
    @Test
    fun `retry is disabled while a refresh is already in flight`() {
        setContent(
            SearchUiState(
                query = "Elden",
                games = games,
                isRefreshing = true,
                refreshError = UiText.StringResource(R.string.search_refresh_error)
            )
        )

        composeRule.onNodeWithText(string(R.string.search_error_action)).assertIsNotEnabled()
    }

    // Spec: one user intent, one navigation effect — and the guard must reopen.
    // Suppression alone is satisfied by a guard that never does, which would leave
    // the user unable to reach Library twice in a session.
    @Test
    fun `empty state CTA suppresses a rapid second tap and reopens after the window`() {
        val events = mutableListOf<SearchUiEvent>()
        setContent(SearchUiState(query = "nothingmatchesthis"), onEvent = { events += it })

        composeRule.onNodeWithText(string(R.string.search_empty_action)).performClick()
        composeRule.onNodeWithText(string(R.string.search_empty_action)).performClick()
        assertEquals(listOf(SearchUiEvent.OnGoToLibraryClicked), events)

        ShadowSystemClock.advanceBy(Duration.ofMillis(SingleClickWindowMillis + 1))
        composeRule.onNodeWithText(string(R.string.search_empty_action)).performClick()

        assertEquals(
            listOf(SearchUiEvent.OnGoToLibraryClicked, SearchUiEvent.OnGoToLibraryClicked),
            events
        )
    }

    // Spec: the results grid shares ONE guard across every card, so two rapid taps
    // on *different* games still emit a single navigation intent. Moving the
    // wrapper inside GameCard — where the click is, so the natural-looking
    // refactor — gives each card its own guard and lets the pair through.
    @Test
    fun `two rapid taps on different result cards emit one navigation intent`() {
        val events = mutableListOf<SearchUiEvent>()
        setContent(SearchUiState(query = "Elden", games = games), onEvent = { events += it })

        composeRule.onNodeWithText("Elden Ring").performClick()
        composeRule.onNodeWithText("Hollow Knight").performClick()

        assertEquals(listOf(SearchUiEvent.OnGameClicked(games[0].id)), events)
    }

    // Spec: loading wins over content, so a query in flight reads as work in
    // progress rather than showing results that answer the previous one. Seeded
    // with results on purpose — with an empty list the absence proves nothing.
    @Test
    fun `loading state shows the skeleton and hides stale results`() {
        setContent(SearchUiState(query = "Elden", games = games, isLoading = true))

        composeRule.onNodeWithTag(SearchLoadingGridTag).assertExists()
        assertAbsent("Elden Ring")
    }

    // Spec: a failed search shows the full error state. Seeded with results so the
    // assertion tests the branch order, not an empty list.
    @Test
    fun `error state replaces the content rather than sitting beside it`() {
        setContent(
            SearchUiState(
                query = "Elden",
                games = games,
                errorMessage = UiText.DynamicString("Network is down")
            )
        )

        composeRule.onNodeWithText("Network is down").assertIsDisplayed()
        assertAbsent("Elden Ring")
    }

    // Spec: a refresh with nothing to keep on screen — Retry after a failed first
    // search — shows the skeleton rather than falling through to "No Results
    // Found", which would read as an answer instead of as work in progress.
    @Test
    fun `refresh with no results to keep shows the skeleton, not the empty state`() {
        setContent(SearchUiState(query = "Elden", isRefreshing = true))

        composeRule.onNodeWithTag(SearchLoadingGridTag).assertExists()
        assertAbsent(string(R.string.search_empty_title))
    }

    // Spec: before any query is typed the screen shows guidance, not an empty grid.
    // The defect this guards was a blank canvas on first open.
    @Test
    fun `initial state shows guidance rather than an empty grid`() {
        setContent(SearchUiState())

        composeRule.onNodeWithText(string(R.string.search_initial_title)).assertIsDisplayed()
    }
}
