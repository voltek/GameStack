---
name: write-tests
description: Write GameStack's JVM tests — UseCases, Repositories, Mappers, Interceptors, ViewModels, Room converters and stored enums, and Compose screens under Robolectric — and see a regression test fail. Use when asked to write, add, or complete tests, when a spec scenario names a test, or to prove a regression test catches its bug.
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

### Room
Not exercised yet: no Entity exists. The first in Detail (#106) validates these; correct
them in that branch from what the code needed.
- **Write-path mapper.** `domain.toEntity().toDomain()` returns the original,
  including each nullable field unset. It proves the mapper's consistency, not how
  Room stores a column.
- **Stored enum.** One test asserting `entries.map { it.name }` equals the names as
  stored, whose failure message says that renaming or removing a stored constant
  needs the app's data cleared before the first release, and a version bump and a
  `Migration` after it (ADR 0068). Room stores the name and no schema check sees a
  rename, so this test is the only thing that does.
- **TypeConverter**, only for a type Room cannot store. Call its functions directly,
  without a `@Database`: value to stored form and back returns the original, and a
  stored form it cannot read has a documented result the test asserts.

## Screen tests
Under `test/` with Robolectric, run by `./gradlew test`:

```kotlin
@RunWith(RobolectricTestRunner::class)
class FooScreenTest {
    @get:Rule val composeRule = createComposeRule()
```

- No `@Config(sdk = …)`: the SDK is pinned once in
  `app/src/test/resources/robolectric.properties`, and a per-class annotation
  overrides that pin.
- Drive the `internal` `{Name}Content` with a `UiState`, collecting events into a
  list: no Hilt graph, no ViewModel, no mocks.
- Cover each state the screen declares, the list the device pass also uses
  (`verify-on-device`), and Composable-level logic no unit test reaches, such as a
  click guard or a `remember`ed derivation; also which controls are enabled and the
  semantics a node carries.
- A screen test cannot show what TalkBack speaks, gestures, the IME or a real device:
  asserting a `liveRegion` is declared is not asserting it is announced, so say which
  one the test proves.
- Screen tests cost about a second each, so adding the second screen-test class is
  the trigger of #67, which splits them into their own Gradle task.

## Naming
Backticks and natural language, `{method or event} should {expected result} when
{condition}`:

```kotlin
fun `invoke should return empty list without calling repository when query is blank`()
fun `handleEvent OnGameClicked should emit NavigateToGameDetail effect`()
```

## Seeing a regression test fail
Why a regression test must be seen to fail, and that the commit says how:
`.claude/rules/tests.md`. A test for brand-new code has no previous behaviour to fail
against; writing the test first and watching it fail gives the same guarantee more
cheaply.

When the fix came first and the old code still compiles, park the fix:

```bash
git stash push -- app/src/main/java/com/gamestack/<path>/Foo.kt   # the fix, never the test
./gradlew testDebugUnitTest --tests "com.gamestack.<path>.FooTest"  # must fail
git stash pop
```

When it does not compile, because the fix removed a `UiEffect`, renamed a field or
changed a signature, break the fix instead: delete or invert one line the new test
claims to cover, run the class, and restore the line. Exactly that test must fail: if
none fails, it proves nothing; if several do, the mutation was too broad to
attribute. This is hand-run mutation testing; PIT, its automated form, is in #64.

Either way:
- Read the failure message: it should describe the bug. A compile error, an NPE or
  MockK's `no answer found for …` is red for the wrong reason. The last is common
  here: `runTest` runs pending work after the body, so a debounced query never meant
  to be dispatched still reaches the mock; stub it, with a comment that the stub
  exists only for that drain.
- Run the one class, not the suite, so the failure is the test's and not something
  disturbed.

## Quality criteria
- Mocks are MockK's: `mockk()`, `every`/`coEvery`, `verify`/`coVerify`.
- No real network or database calls — everything mocked.
- Every public function and every `UiEvent` has a happy and a sad test where it can
  fail.
