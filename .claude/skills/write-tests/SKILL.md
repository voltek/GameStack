---
name: write-tests
description: Write JVM tests — UseCases, Repositories, Mappers, Interceptors, ViewModels, Room converters and stored enums, and Compose screens under Robolectric — and see a regression test fail. Use when asked to write, add, or complete tests, when a spec scenario names a test, or to prove a regression test catches its bug.
---

## Stack and scope
JUnit 4, MockK, Turbine and `kotlinx-coroutines-test`, all under `test/` and run by
`./gradlew test` (`.claude/rules/tests.md`). Robolectric with Compose's test rule only
for screen tests (ADR 0016): never for a DAO against real SQLite, and never for a
test that can run without it.

No test here reaches Room: a DAO is covered through Repository tests with a mocked
DAO, and a converter of a type Room cannot store through direct calls. A wrong
`@Query`, a converter left unregistered and a missing migration, including one an
enum rename needs (ADR 0068), pass every test and fail on device; the instrumented
suite that would catch them is #132. A mocked "DAO test" would only assert the mock,
and would hide that gap rather than show it.

## What a test asserts
The source of truth is the spec or the Skill that described the task, never the code
just written. A test a scenario names in its `VERIFIED BY` line asserts that
scenario's THEN; it is written before its code, under exactly that name, so it can be
seen to fail first. A test no scenario names says in a one-line comment which
requirement it holds. A test that still passes once the intended logic is broken is
tautological: rewrite it against the requirement.

## Setup by layer
- UseCase, Repository, Mapper, Interceptor: no setup; a suspend call runs in
  `runTest`.
- ViewModel: `@get:Rule val mainDispatcherRule = MainDispatcherRule()`
  (`core/testing/`), never `Dispatchers.setMain()` by hand. `runTest` shares the Main
  dispatcher's scheduler, so its `advanceTimeBy()` and `advanceUntilIdle()` also move
  the ViewModel's `delay()`s, as `SearchViewModelTest` does.

## Flows and effects
A `StateFlow` or a Repository stream: Turbine's `.test { awaitItem() }`. A `UiEffect`
travels on a `Channel`, which does not replay, so trigger it inside the block:

```kotlin
viewModel.uiEffect.test {
    viewModel.handleEvent(SearchUiEvent.OnGameClicked(1))
    assertEquals(SearchUiEffect.NavigateToGameDetail(1), awaitItem())
}
```

## What each class gets
- **UseCase.** Happy: the Repository succeeds and the UseCase returns the expected
  data. Sad: the Repository fails and the failure comes out unchanged.
- **Mapper.** What `new-mapper` § Tests lists.
- **Repository.** Happy: each source returns data. Sad: a network failure and an
  empty response.
- **ViewModel.** Every `UiEvent` has a test. Happy: the event produces the expected
  `UiState`. Sad: a UseCase failure reaches `UiState`, or a `UiEffect`.
- **ViewModel that supersedes work.** A `mapLatest` pipeline (`new-viewmodel`), a
  debounce or a throttle also gets an interaction test that the superseded call never
  ran (`coVerify(exactly = 0)`): a cancellation that fails can still leave a
  correct-looking final state. A request superseded after it reached the UseCase has
  already run, so its test advances past when its response was due and asserts the
  result never reached `UiState`, as `SearchViewModelTest` does.
- **Interceptor.** A mocked `Chain`: what it adds to the request, and when it cannot,
  that it throws and `chain.proceed()` is never called.
- **Room.** A write-path mapper, an enum Room stores, a `@TypeConverter`: read
  `references/room-tests.md`.

## Screen tests
A test that drives a Compose screen, under Robolectric: read
`references/screen-tests.md`.

## Naming
Backticks and natural language, `{method or event} should {expected result} when
{condition}`:

```kotlin
fun `invoke should return empty list without calling repository when query is blank`()
fun `handleEvent OnGameClicked should emit NavigateToGameDetail effect`()
```

## Seeing a regression test fail
A test that guards a fix written before it is seen to fail first: read
`references/regression.md` for how.

## Quality criteria
- Mocks are MockK's: `mockk()`, `every`/`coEvery`, `verify`/`coVerify`.
- No real network or database calls — everything mocked.
- Every public function and every `UiEvent` has a happy and a sad test where it can
  fail.
