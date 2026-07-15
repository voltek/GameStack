---
name: write-tests
description: Write unit tests for Domain (UseCases), Data (RepositoryImpl, Mappers, DAO, API), and Presentation (ViewModels) layers. Use when asked to write, add, or complete unit tests for GameStack.
---

## Testing Stack
Turbine, MockK, TestDispatcher

## Setup by layer

### Domain (UseCases)
No special setup needed. Test directly with `runTest`.

### Data (Repository, Mappers, DAO, API)
No special setup needed for Mappers (pure functions).
For Repository/DAO tests involving Flow, use `runTest`.

### Presentation (ViewModels)
Use the project's `MainDispatcherRule` (located in `core/testing/`)
instead of manually calling `Dispatchers.setMain()`/`resetMain()`.

```kotlin
@get:Rule
val mainDispatcherRule = MainDispatcherRule()
```

This rule already wraps `Dispatchers.setMain()` in `starting()`
and `Dispatchers.resetMain()` in `finished()`, using `UnconfinedTestDispatcher`
by default.

## Testing Flow (UiState, Repository streams)
Use Turbine's `.test { }` block:

```kotlin
viewModel.uiState.test {
    val state = awaitItem()
    assertEquals(expected, state)
}
```

## Testing Channel (UiEffect / one-shot events)
Same Turbine pattern, but wrap the triggering action inside the `.test { }` block,
since Channel only emits once and does not replay:

```kotlin
viewModel.uiEffect.test {
    viewModel.handleEvent(SomeEvent)
    val effect = awaitItem()
    assertTrue(effect is ExpectedEffect)
}
```

## What counts as Happy/Sad path per class type

### UseCase
- Happy: repository returns success, UseCase returns/emits the expected data
- Sad: repository returns failure, UseCase propagates the error correctly

### Mapper
- Happy: all fields map correctly with valid input
- Sad: nullable fields produce safe defaults, empty/null lists produce emptyList()

### Repository Impl
- Happy: remote/local source returns data successfully
- Sad: network failure, empty response, cache miss

### ViewModel
- Happy: UiEvent triggers the expected UiState update
- Sad: UseCase failure updates UiState to error and/or sends UiEffect (Snackbar)
- Also test: every UiEvent has at least one corresponding test

## Naming convention
Test function names use backticks with natural language, describing behavior:

```kotlin
fun `invoke should return success flow with games list when repository is successful`()
fun `onEvent OnGameClicked should send NavigateToGameDetails effect`()
```

Pattern: `{method or event} should {expected result} when {condition}`

## Quality criteria
- Every public function/UseCase/ViewModel event has at least one Happy and one Sad path test
- Mocks use MockK exclusively (`mockk()`, `every {}`, `verify {}`, `coEvery {}`, `coVerify {}`)
- No real network or database calls — everything mocked
- Test names follow the naming convention above
