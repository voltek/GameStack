---
name: write-tests
description: Write unit tests for Domain (UseCases), Data (RepositoryImpl, Mappers, Interceptors), and Presentation (ViewModels) layers. Use when asked to write, add, or complete unit tests for GameStack.
compatibility: Requires `MainDispatcherRule` to exist in `core/testing/` for ViewModel tests.
---

## Testing Stack
Turbine, MockK, TestDispatcher

## Scope — JVM unit tests only
Everything this skill writes goes under `test/` and runs on the JVM via
`./gradlew test`: no emulator, no Robolectric, no real database or network.
That boundary is what keeps the suite fast enough to gate every change
(CLAUDE.md, Testing Stack → Tier 1).

**DAOs are therefore not tested directly here.** A `@Dao` interface has no
behavior of its own — Room generates the implementation at compile time, and
exercising it needs real SQLite (an instrumented `androidTest/` suite, currently
backlog). What this skill covers instead:
- Repository tests with a **mocked DAO** — proving the Repository calls the right
  DAO function with the right arguments, and handles what comes back.
- Write-path mapper tests with an enum round trip — proving the *mapper's own*
  field-to-field mapping is consistent (see `new-mapper`). This does NOT exercise
  Room's registered `@TypeConverter` — `toEntity()`/`toDomain()` never call it.
- Direct `@TypeConverter` tests — plain JVM calls against the converter object's
  functions (`GameRatingConverters.fromRating(...)`, `.toRating(...)`, etc.), no
  Room/SQLite involved. This is what actually proves a converter is correct,
  registered, and matches the mapper (see "TypeConverter" below).

Be honest about the residual gap: a wrong `@Query` string or a missing migration
will pass every test in this project and fail on device. Don't write a mocked
"DAO test" that appears to cover this — it would be tautological (see the Oracle
Problem below) and would hide the gap rather than flag it.

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

### Data (Repository, Mappers, Interceptors)
No special setup needed for Mappers (pure functions).
For Repository tests involving Flow, use `runTest` with a mocked DAO/API service.

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

### ViewModel — cancellation/debounce
If the ViewModel stores a `Job` and cancels it (debounce, throttle, superseding
a previous action), the state-based Happy/Sad tests above are not enough —
add an interaction test confirming the superseded call never fired
(`coVerify(exactly = 0)`), not just that the final state looks correct. A
cancellation that silently fails can still produce a correct-looking final
state by coincidence.

### Interceptor
- Happy: both `Client-ID` and `Authorization: Bearer {token}` are added to the
  outgoing request.
- Sad: when the token source fails, `AuthInterceptor` throws `IOException` and the
  request does **not** proceed — assert the throw, and assert `chain.proceed()` was
  never called. This behavior is decided, not open: see CLAUDE.md, Data Sources.
- No Android framework needed — plain JVM unit test, not instrumented.

### Mapper (write path)
- Round trip: `domainModel.toEntity().toDomain()` returns the original model,
  including nullable enum fields (rating unset, list status unset). Proves the
  mapper's internal consistency only — see "TypeConverter" below for what
  actually exercises Room's converter.

### TypeConverter
- Happy: call the `@TypeConverter` function directly (not through Room) —
  enum → stored string → enum returns the original value. No `@Database`,
  no SQLite, no instrumentation needed: it's a plain function call.
- Sad: an unrecognized stored string returns the documented fallback (`null`,
  per `new-room-dao`) instead of throwing.
- This is the test that actually catches a converter storing the wrong
  representation (e.g. ordinal instead of name) — the mapper round trip above
  cannot, because it never calls this function.

## Naming convention
Test function names use backticks with natural language, describing behavior:

```kotlin
fun `invoke should return success flow with games list when repository is successful`()
fun `handleEvent OnGameClicked should send NavigateToGameDetail effect`()
```

Pattern: `{method or event} should {expected result} when {condition}`

## Verifying a regression test actually catches the bug
A test written after the fix can pass whether or not the fix is present. That
test is worse than no test: it looks like coverage and proves nothing. Every
regression test must be *seen* to fail against the code it guards (CLAUDE.md →
Testing Stack, Tier 1).

```bash
# 1. Test and fix are both written and green.
# 2. Park the production change only — never the test.
git stash push -- app/src/main/java/com/gamestack/<path>/Foo.kt

# 3. Run that test class alone. It MUST fail.
./gradlew testDebugUnitTest --tests "com.gamestack.<path>.FooTest"

# 4. Restore.
git stash pop
```

Three things that decide whether this is worth anything:

- **Read the failure message, not just the red.** It should describe the bug. A
  failure from a compile error, an NPE, or MockK's `no answer found for …` is red
  for the wrong reason and proves nothing. (That last one is common here: `runTest`
  drains pending work after the body, so a debounced query you never meant to
  dispatch still reaches the mock — stub it, and say in a comment that the stub
  exists only for the drain.)
- **Run the one class, not the suite.** Otherwise you cannot tell your test failed
  rather than something you disturbed.
- **This applies to regression tests, not to tests for brand-new code** — those
  have no previous behaviour to fail against, and would just fail to compile.

### When stashing cannot work: targeted mutation
The stash recipe assumes the old code still *compiles* against the current
contract. That holds for a patch and fails for a redesign: if the fix removed a
`UiEffect`, renamed a field, or changed a signature, restoring the old file
breaks the build — red for the wrong reason, which the first bullet above already
rejects.

The equivalent at that scale is to break the fix rather than remove it. Delete or
invert **one line** that the new test depends on, run the class, and confirm that
exactly the expected test fails:

```bash
# e.g. delete `refreshError = null` from the success branch,
# then run the class and expect exactly one failure.
./gradlew testDebugUnitTest --tests "com.gamestack.<path>.FooTest"
```

Then restore the line. Two rules make it worth as much as the stash version:

- **One line, chosen because a specific test claims to cover it.** Mutating
  several at once, or picking a line at random, only tells you the suite is
  non-empty.
- **Exactly the expected test must fail.** If none fails, that test proves
  nothing. If several fail, the mutation was too broad to attribute.

This is hand-run mutation testing, which is also the automated technique (PIT)
named in `docs/project/TOOLING-BACKLOG.md` — the manual version is what to
reach for until that lands.

Writing the test first (watch it fail, then fix) gives you the same guarantee and
is cheaper. This recipe is for when the fix came first, which in practice is
often. Say in the commit message that it was verified.

The automated generalization of this idea is **mutation testing** (PIT on the
JVM): mutate production code, assert some test goes red. Not adopted here — its
Android setup is awkward and slow — but it is the same question asked at scale.

## Quality criteria
- Every public function/UseCase/ViewModel event has at least one Happy and one Sad path test
- Mocks use MockK exclusively (`mockk()`, `every {}`, `verify {}`, `coEvery {}`, `coVerify {}`)
- No real network or database calls — everything mocked
- Test names follow the naming convention above

## Known gaps (deliberate, not oversights)
- **Regression pillar.** This skill covers Requirements testing only (new code
  does what the Skill/Spec asked). Regression testing (confirm nothing else
  broke) is not enforced here — it will be automated via a Hook that runs the
  affected suite after each agent edit — see `docs/project/TOOLING-BACKLOG.md`.
- **Real persistence.** No test in this project touches real SQLite; see
  "Scope" above. An instrumented `androidTest/` suite covering DAO queries and
  Room migrations is backlog (Spec → Explicitly Deferred). `@TypeConverter`s
  are the exception — they're plain JVM functions and ARE directly unit-tested
  (see "TypeConverter" above); what remains untested pre-instrumentation is
  Room actually invoking them at runtime (registration on `@Database`) and the
  `@Query`/migration surface.
