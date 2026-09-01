# DRIFT-CHECKLIST — GameStack

> Manual audit checklist derived from CLAUDE.md's Tier 1 (Immutable) rules.
> Run this periodically, or after a batch of agent-generated features, to detect
> drift between the code and the Project Constitution.
>
> Each item is phrased as a verifiable question, and **every item is written so
> that "no" is the healthy answer** — a "yes" means drift was found. Read them as
> violation detectors, not as goals.
>
> When drift is found, resolve it per the Self-Healing Loop (CLAUDE.md): either
> fix the code, OR update the document if the code turned out to be right and the
> document is what's outdated. Log which direction was chosen in
> `docs/project/DRIFT-LOG.md`; a decision that is born or changes goes to
> `docs/project/decisions/` as an ADR instead.
>
> Future: this checklist becomes the logic for an automated checker (Hook /
> validator subagent) — see `docs/project/TOOLING-BACKLOG.md`.

---

## Tech Stack
- [ ] Is there any XML layout or ViewBinding usage in a UI screen (instead of Compose)?
- [ ] Is there any HTTP client other than Retrofit/OkHttp, or a serialization
      library other than Kotlinx.serialization (e.g. Gson, Moshi)?
- [ ] Is there any raw string navigation route (instead of Navigation Compose Safe-type)?
- [ ] Has a Light ColorScheme, a `dynamicColor` parameter, or a
      `dynamicDarkColorScheme()` call appeared anywhere? MVP is dark-only with a
      fixed palette — all three are drift, not progress.
- [ ] For any library added recently — was a remembered/guessed version used
      instead of checking the latest stable one?

## Testing Stack
- [ ] Does any ViewModel test call `Dispatchers.setMain()`/`resetMain()` manually
      instead of using `MainDispatcherRule`?
- [ ] Is any mock created with something other than MockK?
- [ ] Is any Flow/Channel asserted without Turbine's `.test { }` block?
- [ ] Has any test under `test/` started requiring an emulator, a real database,
      or a real network call? Or Robolectric anywhere other than a Compose screen
      test? (JVM-only is the gate — see CLAUDE.md.)
- [ ] Does `app/src/test/resources/robolectric.properties`'s `sdk=` disagree with
      `targetSdk`? Nothing enforces the match, and the failure is silent: screen
      tests keep passing on the older API while the app ships the newer one.
- [ ] Does any document record an exact measured figure (timings, counts,
      percentages) that nothing regenerates? Three went stale within one branch.
      State the shape and the command to re-derive it instead.
- [ ] Is there a "DAO test" that only asserts against its own mock? Tautological —
      it proves nothing and hides the real coverage gap.
- [ ] Did any regression test land without being seen to fail against the code it
      guards? A test written after the fix can pass either way — same failure
      mode as the tautological DAO test above, one layer up.

## Architecture
- [ ] Does any ViewModel or Composable call a Repository function directly,
      skipping the UseCase?
- [ ] Does any Repository implementation contain business logic beyond
      "coordinate data sources" — calculating derived values or applying domain
      rules? (Deliberate exception, not drift: the `updatedAt`/`completedAt`
      write rules on `UserGameEntity`. Everything else still counts.)
- [ ] Does any file under `domain/` import `retrofit2.*`, `androidx.room.*`,
      or any `android.*` package (other than `javax.inject`)?
- [ ] Is `LiveData` used anywhere in the codebase?
- [ ] Does any Composable contain logic beyond view/rendering logic
      (e.g. business rules, direct data transformation)?
- [ ] Does the bottom navigation have anything other than exactly 3 destinations,
      or has Detail been promoted into it?
- [ ] Is `Detail` registered once at top level instead of inside every tab's
      nested graph, or does any bottom nav item / `navigateToBottomNavDestination()`
      call target a tab's screen route instead of its `*Graph` route? Either one
      breaks per-tab back stacks (Detail leaks across tabs, no tab selected).
- [ ] Does any navigation code call `NavController.currentBackStack`? It is
      `@RestrictTo` library-group API and fails the `lint` gate.
- [ ] Does any suspend function build a `Result` with `runCatching`? It swallows
      `CancellationException`, reporting a cancelled coroutine as a real failure.
- [ ] Are any `UiState`/`UiEvent`/`UiEffect` types declared outside their screen's
      `{ScreenName}Contract.kt`?
- [ ] Is any one-shot event (navigation, snackbar) modeled in `UiState` instead of
      `UiEffect`? It will re-fire on every recomposition.
- [ ] Does any ViewModel expose a public entry point other than `handleEvent()`?

## Data Sources
- [ ] Does any IGDB endpoint use @GET or @Query instead of @POST with a plain-text Apicalypse body?
- [ ] Does any IGDB endpoint declare `Client-ID`/`Authorization` as `@Header`
      parameters, or does any UseCase/ViewModel fetch a token manually?
      `AuthInterceptor` is the only place either may happen.
- [ ] Has a second IGDB Retrofit instance or OkHttp client appeared, bypassing
      the one that carries `AuthInterceptor`?
- [ ] Is the personal rating/list data (Room) ever being sent to or merged
      with IGDB's community rating anywhere in the code?
- [ ] Does any Room enum column lack a `TypeConverter`, or does a converter
      persist the enum's **ordinal** rather than its name?
- [ ] Was an Entity's schema changed without bumping the Database `version`?
- [ ] Is `fallbackToDestructiveMigration()` still present now that the app has
      shipped to real users? (Fine pre-release; data loss after.)

## Code Conventions
- [ ] Is any class not PascalCase, or any function not camelCase?
- [ ] Does any DTO or Entity type appear in a signature reachable from the Domain
      or UI layer? (Both `toDomain()` and `toEntity()` are legitimate — the rule
      is about where the *types* surface, not which direction the mapper runs.)
- [ ] Does any DTO → Entity mapper exist? (Must convert through the Domain model.)
- [ ] Does any mapper read the clock (`System.currentTimeMillis()`) or stored
      state, instead of being a pure field-for-field conversion?
- [ ] Is there any code comment or identifier written in Spanish?
- [ ] Does any comment restate what the code says, narrate *how we got here*
      (a past bug, an earlier version, which review found it), or duplicate text
      that already exists in a Skill or the Resolution log? Any comment block
      over 4 lines, or a file past ~15% comment lines, is where to look first.
- [ ] Is there any hardcoded UI string in a ViewModel or UseCase instead of `UiText`?
- [ ] Is any Hilt module named for a layer or lifetime rather than a concern
      (`AppModule`, `CommonModule`), or does any `@Provides` exist for a class
      that is already constructor-injectable?

## Documentation integrity
These check the harness itself, not the code — the failure mode that produced
this checklist's own past drift.
- [ ] Does any document reference a component, Skill, principle, file or section
      that isn't defined anywhere or marked `(PENDING — not yet defined)`?
      Deleting a section is the usual cause — check what pointed at it.
- [ ] Does any document use course or study vocabulary ("Block N", phase
      numbering, learning objectives) rather than naming the work itself?
      That belongs to the course, which is kept outside this repository.
- [ ] Does CLAUDE.md's "Available Skills" list match the actual contents of
      `.claude/skills/`, and the Spec's copy of that list? (Retired Skills are
      listed separately in CLAUDE.md and archived under
      `docs/project/retired-skills/` — they must not appear as invocable.)
- [ ] Does DESIGN.md's prose reference a token its own token block doesn't define,
      or state a value that contradicts it (colors, radii, type roles)?
- [ ] Did a recent implementation decision (a fallback, a mapping, a naming rule)
      get made in code without being written down anywhere?
