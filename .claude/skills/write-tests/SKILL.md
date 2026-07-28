---
name: write-tests
description: Write unit tests for Domain (UseCases), Data (RepositoryImpl, Mappers, DAO, API), and Presentation (ViewModels) layers. Use when asked to write, add, or complete unit tests for GameStack.
compatibility: Requires `MainDispatcherRule` to exist in `core/testing/` for ViewModel tests.
---

## Testing Stack
Turbine, MockK, TestDispatcher

## Oracle Problem — what "correct" means
The source of truth for a test is the Skill or Spec that described the task —
never the implementation you just wrote. Before writing assertions, restate
in a comment what the Skill/Spec required, then assert against that, not
against whatever the generated code currently returns.

Red flag: if a test would still pass after silently breaking the intended
logic, it's tautological — rewrite it against the actual requirement.

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

## Pending — Regression pillar
This skill covers Requirements testing only (new code does what the Skill/Spec
asked). Regression testing (confirm nothing else broke) is not enforced here —
it will be automated via a Hook in Block 4, running the affected test suite
automatically after each agent edit.
