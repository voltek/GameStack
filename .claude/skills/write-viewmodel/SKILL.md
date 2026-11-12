---
name: write-viewmodel
description: Create a ViewModel following the MVI pattern for a screen, or change one. Use when asked to create a ViewModel, or to add debounced input or a navigation argument to one.
---

## Construction
- `@HiltViewModel`, with its UseCase(s) injected through an `@Inject` constructor.
- Three contracts are the only way the UI and the ViewModel communicate, all in
  `{ScreenName}Contract.kt`:
  - `UiState`, a data class exposed as `StateFlow` over a private `MutableStateFlow`.
    A sealed hierarchy only for states that truly exclude each other: loading,
    content and error often coexist, as when a refresh fails over results still on
    screen (ADR 0017).
  - `UiEvent`, a sealed class of everything the UI can send, each named for what the
    user did: `OnQueryChanged`, `OnRefresh`, `OnGameClicked`.
  - `UiEffect`, a sealed class of one-shot events, sent on a `Channel` and exposed
    with `receiveAsFlow()` (ADR 0011, under evaluation in #175).
- One public entry point, `fun handleEvent(event: UiEvent)`, whose `when` routes to
  private handlers.
- Change state with `.update { }`.
- Text for the user is `UiText` in `UiState` and `UiEffect`, never a raw string.
  `UiText` is an Android type, so a UseCase returns no text for the user.
- A screen with a navigation argument reads it with
  `savedStateHandle.toRoute<Destination.Detail>()`, as Android's type-safe navigation
  guide does. Not exercised yet (#106): no screen takes
  an argument, and no test builds its `SavedStateHandle`.
  The first branch to run it corrects it and removes this note.

## Location and names
`feature/{name}/presentation/`: `{ScreenName}ViewModel.kt`, and `{ScreenName}Contract.kt`
holding `{ScreenName}UiState`, `{ScreenName}UiEvent` and `{ScreenName}UiEffect`, as
Search does.

## Debounced input
Input that waits for the user to stop typing is a flow pipeline, never a `Job` plus
fields remembering which query was last dispatched: such a field drifts from what
the screen shows. `SearchViewModel` is the worked example:

```kotlin
merge(typedQuery.map { it.trim() }.distinctUntilChanged().map { Request(it, isRefresh = false) },
      refreshRequests.map { Request(typedQuery.value.trim(), isRefresh = true) })
    .mapLatest { request ->
        delay(request.debounceMillis)   // 0 for a refresh or an emptied field
        if (request.query.isNotEmpty()) performSearch(request.query, request.isRefresh)
    }
    .launchIn(viewModelScope)
```

- `distinctUntilChanged` after `trim`: a whitespace-only edit is not a new query, so
  it neither restarts a search nor disturbs one in flight.
- `mapLatest` cancels the previous request, whether it is waiting out its debounce
  or awaiting a response. The empty query travels the pipeline too, so deleting the
  text cancels its search.
- The wait goes inside `mapLatest`, not in a `debounce()` upstream of it. `debounce`
  emits a value only once its timeout passes with no newer one, so until then
  `mapLatest` has not seen the new text and the previous search keeps running; its
  response can still land and paint results or the error for text already replaced.
- When the trimmed query changes, no state of the previous query remains (results,
  errors, the refresh flag), and `isLoading` is true if and only if the new query is
  non-empty, since its search is now pending. The event handler sets both at once:
  the skeleton then shows during the debounce, and an emptied field runs no search
  that would reset them. `SearchViewModel.onQueryChanged` is the worked example, with
  one branch for an emptied field, one for a new query, and one for a whitespace-only
  edit, which updates only the text shown.

## Quality criteria
- No product rules: the ViewModel calls UseCases and maps their results to
  `UiState` (`.claude/rules/code.md`).
- Every `UiEvent` has a handler, and every failure a UseCase returns reaches
  `UiState`, or a `UiEffect` when it is a one-shot event.
- Invoke `write-tests` for its unit tests.
