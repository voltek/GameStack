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
> Every item carries a **`Verified by:`** line naming what checks it *today*,
> not what could check it one day. The vocabulary is closed:
>
> - `human` — a person reads the code or the documents and decides. This is the
>   default, and it says nothing about whether the item *could* be mechanised:
>   most of these could be, and none of them are yet.
> - `subagent (interpretive)` — the question needs judgment to answer at all, so
>   no test or script will ever settle it on its own. A claim about the item's
>   nature, not about today's tooling.
> - `./gradlew lint`
> - `test: <TestName>#<caseName>` — spelled as `decisions/README.md` spells it,
>   so the two vocabularies can be joined where they overlap.
>
> That is what makes this file the coverage map for the automated checker in
> `docs/project/TOOLING-BACKLOG.md` (item 3) rather than just its wish list: the
> `human` lines are the work it can take over, the `subagent (interpretive)`
> lines are the work it cannot, and a line already naming a tool is work it does
> not need to. Read the map with
> `grep -o "^ *Verified by: .*" docs/project/DRIFT-CHECKLIST.md | sort | uniq -c`.

---

## Tech Stack
- [ ] Is there any XML layout or ViewBinding usage in a UI screen (instead of Compose)?
      Verified by: human
- [ ] Is there any HTTP client other than Retrofit/OkHttp, or a serialization
      library other than Kotlinx.serialization (e.g. Gson, Moshi)?
      Verified by: human
- [ ] Is there any raw string navigation route (instead of Navigation Compose Safe-type)?
      Verified by: human
- [ ] Has a Light ColorScheme, a `dynamicColor` parameter, or a
      `dynamicDarkColorScheme()` call appeared anywhere? MVP is dark-only with a
      fixed palette — all three are drift, not progress.
      Verified by: human
- [ ] For any library added recently — was a remembered/guessed version used
      instead of checking the latest stable one?
      Verified by: human

## Testing Stack
- [ ] Does any ViewModel test call `Dispatchers.setMain()`/`resetMain()` manually
      instead of using `MainDispatcherRule`?
      Verified by: human
- [ ] Is any mock created with something other than MockK?
      Verified by: human
- [ ] Is any Flow/Channel asserted without Turbine's `.test { }` block?
      Verified by: human
- [ ] Has any test under `test/` started requiring an emulator, a real database,
      or a real network call? Or Robolectric anywhere other than a Compose screen
      test? (JVM-only is the gate — see CLAUDE.md.)
      Verified by: human
- [ ] Does `app/src/test/resources/robolectric.properties`'s `sdk=` disagree with
      `targetSdk`? Nothing enforces the match, and the failure is silent: screen
      tests keep passing on the older API while the app ships the newer one.
      Verified by: human
- [ ] Does any document record an exact measured figure (timings, counts,
      percentages) that nothing regenerates? Three went stale within one branch.
      State the shape and the command to re-derive it instead.
      Verified by: subagent (interpretive)
- [ ] Is there a "DAO test" that only asserts against its own mock? Tautological —
      it proves nothing and hides the real coverage gap.
      Verified by: subagent (interpretive)
- [ ] Did any regression test land without being seen to fail against the code it
      guards? A test written after the fix can pass either way — same failure
      mode as the tautological DAO test above, one layer up.
      Verified by: subagent (interpretive)

## Architecture
- [ ] Does any ViewModel or Composable call a Repository function directly,
      skipping the UseCase?
      Verified by: human
- [ ] Does any Repository implementation contain business logic beyond
      "coordinate data sources" — calculating derived values or applying domain
      rules? (Deliberate exception, not drift: the `updatedAt`/`completedAt`
      write rules on `UserGameEntity`. Everything else still counts.)
      Verified by: subagent (interpretive)
- [ ] Does any file under `domain/` import `retrofit2.*`, `androidx.room.*`,
      or any `android.*` package (other than `javax.inject`)?
      Verified by: human
- [ ] Is `LiveData` used anywhere in the codebase?
      Verified by: human
- [ ] Does any Composable contain logic beyond view/rendering logic
      (e.g. business rules, direct data transformation)?
      Verified by: subagent (interpretive)
- [ ] Does the bottom navigation have anything other than exactly 3 destinations,
      or has Detail been promoted into it?
      Verified by: human
- [ ] Is `Detail` registered once at top level instead of inside every tab's
      nested graph, or does any bottom nav item / `navigateToBottomNavDestination()`
      call target a tab's screen route instead of its `*Graph` route? Either one
      breaks per-tab back stacks (Detail leaks across tabs, no tab selected).
      Verified by: human
- [ ] Does any navigation code call `NavController.currentBackStack`? It is
      `@RestrictTo` library-group API and fails the `lint` gate.
      Verified by: ./gradlew lint
- [ ] Does any suspend function build a `Result` with `runCatching`? It swallows
      `CancellationException`, reporting a cancelled coroutine as a real failure.
      Verified by: human
- [ ] Are any `UiState`/`UiEvent`/`UiEffect` types declared outside their screen's
      `{ScreenName}Contract.kt`?
      Verified by: human
- [ ] Is any one-shot event (navigation, snackbar) modeled in `UiState` instead of
      `UiEffect`? It will re-fire on every recomposition.
      Verified by: subagent (interpretive)
- [ ] Does any ViewModel expose a public entry point other than `handleEvent()`?
      Verified by: human

## Data Sources
- [ ] Does any IGDB endpoint use @GET or @Query instead of @POST with a plain-text Apicalypse body?
      Verified by: human
- [ ] Does any IGDB endpoint declare `Client-ID`/`Authorization` as `@Header`
      parameters, or does any UseCase/ViewModel fetch a token manually?
      `AuthInterceptor` is the only place either may happen.
      Verified by: human
- [ ] Has a second IGDB Retrofit instance or OkHttp client appeared, bypassing
      the one that carries `AuthInterceptor`?
      Verified by: human
- [ ] Is the personal rating/list data (Room) ever being sent to or merged
      with IGDB's community rating anywhere in the code?
      Verified by: subagent (interpretive)
- [ ] Does any Room enum column lack a `TypeConverter`, or does a converter
      persist the enum's **ordinal** rather than its name?
      Verified by: human
- [ ] Was an Entity's schema changed without bumping the Database `version`?
      Verified by: human
- [ ] Is `fallbackToDestructiveMigration()` still present now that the app has
      shipped to real users? (Fine pre-release; data loss after.)
      Verified by: human

## Code Conventions
- [ ] Is any class not PascalCase, or any function not camelCase?
      Verified by: human
- [ ] Does any DTO or Entity type appear in a signature reachable from the Domain
      or UI layer? (Both `toDomain()` and `toEntity()` are legitimate — the rule
      is about where the *types* surface, not which direction the mapper runs.)
      Verified by: human
- [ ] Does any DTO → Entity mapper exist? (Must convert through the Domain model.)
      Verified by: human
- [ ] Does any mapper read the clock (`System.currentTimeMillis()`) or stored
      state, instead of being a pure field-for-field conversion?
      Verified by: human
- [ ] Is there any code comment or identifier written in Spanish?
      Verified by: human
- [ ] Does any comment restate what the code says, narrate *how we got here*
      (a past bug, an earlier version, which review found it), or duplicate text
      that already exists in a Skill, DRIFT-LOG or an ADR? Any comment block
      over 4 lines, or a file past ~15% comment lines, is where to look first.
      Verified by: subagent (interpretive)
- [ ] Is there any hardcoded UI string in a ViewModel or UseCase instead of `UiText`?
      Verified by: human
- [ ] Is any Hilt module named for a layer or lifetime rather than a concern
      (`AppModule`, `CommonModule`), or does any `@Provides` exist for a class
      that is already constructor-injectable?
      Verified by: human

## Documentation integrity
These check the harness itself, not the code — the failure mode that produced
this checklist's own past drift.
- [ ] Does any document reference a component, Skill, principle, file or section
      that isn't defined anywhere or marked `(PENDING — not yet defined)`?
      Deleting a section is the usual cause — check what pointed at it.
      Verified by: human
- [ ] Does any document use course or study vocabulary ("Block N", phase
      numbering, learning objectives) rather than naming the work itself?
      That belongs to the course, which is kept outside this repository.
      Verified by: human
- [ ] Does CLAUDE.md's "Available Skills" list differ from the actual contents
      of `.claude/skills/`, or from the Spec's copy of that list? (Retired
      Skills are listed separately in CLAUDE.md and archived under
      `docs/project/retired-skills/` — one of those appearing as invocable is
      the same drift.)
      Verified by: human
- [ ] Does DESIGN.md's prose reference a token its own token block doesn't define,
      or state a value that contradicts it (colors, radii, type roles)?
      Verified by: human
- [ ] Did a recent implementation decision (a fallback, a mapping, a naming rule)
      get made in code without being written down anywhere?
      Verified by: subagent (interpretive)
- [ ] Does any ADR under `docs/project/decisions/` break the field rules its own
      README sets — a missing `Backfilled:` or `Enforced by:` line, an
      `Enforced by:` value outside the closed vocabulary, a `Date:` that
      disagrees with the row `Backfilled:` cites, or quoted opening words that
      no `grep -F` against `DRIFT-LOG.md` resolves? Three addressing rules, each
      earned by a false result: anchor `Enforced by:` at line start and read
      every one of them, because ADR prose quotes the field name too; resolve
      *every* row a `Backfilled:` line cites, not only the first; and take the
      cited date as the one immediately before the quoted words, which may
      carry a date of their own.
      Verified by: human
