---
name: new-viewmodel
description: Create a ViewModel following the MVI pattern for a GameStack screen. Use when asked to create a ViewModel, or as part of the new-feature skill.
---

## Construction
- Annotated with `@HiltViewModel`, constructor injected with the required UseCase(s) via `@Inject`.
- Defines three contracts — full rules in CLAUDE.md, Architecture →
  "MVI Contract conventions":
  - `UiState` — data class exposed as `StateFlow`, private `MutableStateFlow` backing it.
    Use a sealed class only when states are truly mutually exclusive, otherwise use data classes.
  - `UiEvent` — sealed class with all actions the UI can send.
  - `UiEffect` — sealed class for one-shot events, exposed via `Channel`/`receiveAsFlow()`.

  All three go in `feature/{name}/presentation/{ScreenName}Contract.kt`, not in
  the ViewModel file — see Location below.
- Single public entry point: `fun handleEvent(event: UiEvent)`, using a `when` to route to
  private handler functions.
- Use `.update { }` on the StateFlow to mutate state immutably.
- Use `UiText` (never raw strings) for any text exposed in UiState or UiEffect.

## Location
- ViewModel: `feature/{feature-name}/presentation/{ScreenName}ViewModel.kt`
- Contracts: `feature/{feature-name}/presentation/{ScreenName}Contract.kt`
  (all three in one file — the one-class-per-file exception in CLAUDE.md,
  Code Conventions → Tier 2)

## Naming convention
`{ScreenName}ViewModel`, `{ScreenName}UiState`, `{ScreenName}UiEvent`, `{ScreenName}UiEffect`
Example: `PopularGamesViewModel`, `PopularGamesUiState`, `PopularGamesUiEvent`, `PopularGamesUiEffect`

## Quality criteria
- No business logic — the ViewModel only orchestrates UseCase calls and maps results to UiState.
- Every UiEvent has a corresponding handler.
- Every error path from a UseCase updates UiState and/or triggers a UiEffect.
- After creating the ViewModel, invoke the `write-tests` skill to generate its corresponding unit tests.
