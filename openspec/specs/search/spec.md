# search Specification

## Purpose
Lets the user find a game in IGDB by its name and open its detail, from the Search tab.

## Requirements

### Requirement: Search by name
The app SHALL search IGDB for the text the user types, once typing pauses, and show at most 20 matches, in IGDB's relevance order. Surrounding whitespace SHALL be ignored, and a blank query SHALL make no request.

#### Scenario: Matches appear once typing pauses
- **WHEN** the user types a game's name and stops typing
- **THEN** the games IGDB matches to that name are shown
- **VERIFIED BY** test: `SearchViewModelTest` › `handleEvent OnQueryChanged should populate games after debounce when search succeeds`

#### Scenario: Relevance order is kept
- **WHEN** IGDB returns its matches
- **THEN** they are shown in the order IGDB ranked them, never re-sorted
- **VERIFIED BY** test: `IgdbApiServiceQueryBuilderTest` › `buildSearchGamesQuery should never include a sort clause`
- **VERIFIED BY** test: `GamesRepositoryImplTest` › `searchGames should keep the order IGDB returns`
- **VERIFIED BY** test: `SearchGamesUseCaseTest` › `invoke should keep the order the repository returns`

#### Scenario: At most 20 matches are shown
- **WHEN** IGDB has more than 20 matches for the text
- **THEN** the first 20 are shown, with no paging
- **VERIFIED BY** test: `IgdbApiServiceQueryBuilderTest` › `buildSearchGamesQuery should cap the results at 20`

#### Scenario: A blank query makes no request
- **WHEN** the query holds only whitespace
- **THEN** no request is made and no results are shown
- **VERIFIED BY** test: `SearchGamesUseCaseTest` › `invoke should return empty list without calling repository when query is blank`

#### Scenario: Whitespace alone does not search again
- **WHEN** the user adds or removes only surrounding whitespace
- **THEN** the trimmed text is what is searched, and no second search is made
- **VERIFIED BY** test: `SearchViewModelTest` › `handleEvent OnQueryChanged should not repeat a search when only whitespace changes`

#### Scenario: A quote in the name is searched as typed
- **WHEN** the query contains a double quote
- **THEN** the search is for that exact text
- **VERIFIED BY** test: `IgdbApiServiceQueryBuilderTest` › `buildSearchGamesQuery should escape double quotes in the query`

#### Scenario: Newer text supersedes an older search
- **WHEN** the user changes the text while a search for the older text is in flight
- **THEN** the older search is cancelled and its results are never shown
- **VERIFIED BY** test: `SearchViewModelTest` › `handleEvent OnQueryChanged should cancel an in-flight search immediately when the query changes`

### Requirement: Result cards
Each result SHALL show the game's cover, its name, its primary genre and its developer. A missing genre or developer SHALL leave that line out rather than show a placeholder.

#### Scenario: A card shows the four fields
- **WHEN** a result is shown
- **THEN** its card shows the cover, the first of its genres, the name and the company IGDB flags as its developer
- **VERIFIED BY** test: `SearchScreenTest` › `result card shows the first genre, the name and the developer`
- **VERIFIED BY** device

#### Scenario: A missing genre or developer is left out
- **WHEN** a game has no genre and no developer
- **THEN** its card shows the name alone, with no placeholder line
- **VERIFIED BY** test: `SearchScreenTest` › `result card leaves out a missing genre and developer`

#### Scenario: The developer is the flagged company
- **WHEN** a game has several involved companies
- **THEN** the developer shown is the one IGDB flags as developer
- **VERIFIED BY** test: `GameDtoMapperTest` › `toDomain should pick the company flagged developer when multiple involved companies exist`

### Requirement: Screen states
Before any query the screen SHALL show guidance; while a search runs, a loading skeleton; then the results, "No Results Found", or an error that replaces the content.

#### Scenario: Initial state shows guidance
- **WHEN** the screen opens with no query
- **THEN** it shows "Search for games" and its guidance, not an empty grid
- **VERIFIED BY** test: `SearchScreenTest` › `initial state shows guidance rather than an empty grid`

#### Scenario: Loading shows the skeleton
- **WHEN** a search is running
- **THEN** a skeleton grid is shown and no stale results
- **VERIFIED BY** test: `SearchScreenTest` › `loading state shows the skeleton and hides stale results`

#### Scenario: No results offers the Library
- **WHEN** a search returns no games
- **THEN** "No Results Found" is shown with Go To Library, which opens the Library tab
- **VERIFIED BY** test: `SearchScreenTest` › `empty state CTA suppresses a rapid second tap and reopens after the window`
- **VERIFIED BY** test: `SearchViewModelTest` › `handleEvent OnGoToLibraryClicked should emit NavigateToLibrary effect`
- **VERIFIED BY** device

#### Scenario: A failed search shows the error state
- **WHEN** a search for new text fails
- **THEN** the error state replaces the content, with Retry, and the previous query's results are not shown
- **VERIFIED BY** test: `SearchScreenTest` › `error state replaces the content rather than sitting beside it`
- **VERIFIED BY** test: `SearchViewModelTest` › `handleEvent OnRefresh should not restore the previous query's results when a new search fails`

### Requirement: Refresh keeps what still answers the query
Pull-to-refresh and Retry SHALL repeat the current query. A failed refresh SHALL keep the results on screen, since they still answer the query, and report the failure as a banner above them, with Retry, until a refresh succeeds or the query changes. The banner reports a condition, so it SHALL never be a transient toast. A failed search for new text is not a refresh: the results on screen answer the previous query.

#### Scenario: Pull to refresh repeats the query
- **WHEN** the user pulls down on the results
- **THEN** the current query is searched again
- **VERIFIED BY** device

#### Scenario: A failed refresh keeps the results
- **WHEN** a refresh fails while results are on screen
- **THEN** the results stay and a banner above them reports the failure, with Retry
- **VERIFIED BY** test: `SearchScreenTest` › `refresh error state shows the banner above the results, not instead of them`

#### Scenario: The banner goes when a refresh succeeds
- **WHEN** a later refresh succeeds
- **THEN** the banner disappears on its own
- **VERIFIED BY** test: `SearchViewModelTest` › `handleEvent OnRefresh should clear the refresh error once a later refresh succeeds`

#### Scenario: The banner goes when the query changes
- **WHEN** the user changes the query while the banner is shown
- **THEN** the banner disappears
- **VERIFIED BY** test: `SearchViewModelTest` › `handleEvent OnQueryChanged should clear the refresh error when the query changes`

#### Scenario: A failed refresh with nothing to keep shows the error state
- **WHEN** a refresh fails and no results are on screen
- **THEN** the error state is shown
- **VERIFIED BY** test: `SearchViewModelTest` › `handleEvent OnRefresh should show the error state when a refresh fails with no results`

#### Scenario: A refresh with nothing to keep shows the skeleton
- **WHEN** a refresh runs and no results are on screen
- **THEN** the loading skeleton is shown, not "No Results Found"
- **VERIFIED BY** test: `SearchScreenTest` › `refresh with no results to keep shows the skeleton, not the empty state`

#### Scenario: Retry waits for the refresh in flight
- **WHEN** a refresh is already running
- **THEN** Retry is disabled and a repeat request is ignored
- **VERIFIED BY** test: `SearchScreenTest` › `retry is disabled while a refresh is already in flight`
- **VERIFIED BY** test: `SearchViewModelTest` › `handleEvent OnRefresh should ignore a repeat request while one is already in flight`

### Requirement: Clear the query
The search bar's clear action SHALL empty the query and return the screen to its initial state.

#### Scenario: Clearing resets the screen
- **WHEN** the user taps the clear button
- **THEN** the query and the results are emptied, a failure on screen goes with them, and the initial guidance shows again
- **VERIFIED BY** test: `SearchViewModelTest` › `handleEvent OnClearQuery should reset query and games`
- **VERIFIED BY** test: `SearchViewModelTest` › `handleEvent OnClearQuery should return to the initial state after a failed search`
- **VERIFIED BY** test: `SearchScreenTest` › `initial state shows guidance rather than an empty grid`

### Requirement: Open a game's detail
Tapping a result SHALL open that game's Detail inside the Search tab, once per tap.

#### Scenario: A tap opens the detail
- **WHEN** the user taps a result card
- **THEN** that game's Detail opens
- **VERIFIED BY** test: `SearchViewModelTest` › `handleEvent OnGameClicked should emit NavigateToGameDetail effect`

#### Scenario: Two rapid taps open one detail
- **WHEN** the user taps two result cards in quick succession
- **THEN** only one Detail opens
- **VERIFIED BY** test: `SearchScreenTest` › `two rapid taps on different result cards emit one navigation intent`
