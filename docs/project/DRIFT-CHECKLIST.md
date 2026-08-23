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
> document is what's outdated. Log which direction was chosen below.
>
> Future: this checklist becomes the logic for an automated checker (Hook /
> validator subagent) in Block 4 — Loop Engineering (defined in CLAUDE.md →
> Pending/Roadmap).

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
- [ ] Has any test under `test/` started requiring Robolectric, an emulator, a real
      database, or a real network call? (JVM-only is the gate — see CLAUDE.md.)
- [ ] Is there a "DAO test" that only asserts against its own mock? Tautological —
      it proves nothing and hides the real coverage gap.

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
- [ ] Is there any hardcoded UI string in a ViewModel or UseCase instead of `UiText`?
- [ ] Is any Hilt module named for a layer or lifetime rather than a concern
      (`AppModule`, `CommonModule`), or does any `@Provides` exist for a class
      that is already constructor-injectable?

## Documentation integrity
These check the harness itself, not the code — the failure mode that produced
this checklist's own past drift.
- [ ] Does any document reference a component, Skill, principle, or "Block N"
      that isn't defined anywhere or marked `(PENDING — not yet defined)`?
- [ ] Does CLAUDE.md's "Available Skills" list match the actual contents of
      `.claude/skills/`, and the Spec's copy of that list? (Retired Skills are
      listed separately in CLAUDE.md and archived under
      `docs/project/retired-skills/` — they must not appear as invocable.)
- [ ] Does DESIGN.md's prose reference a token its own token block doesn't define,
      or state a value that contradicts it (colors, radii, type roles)?
- [ ] Did a recent implementation decision (a fallback, a mapping, a naming rule)
      get made in code without being written down anywhere?

---

## Resolution log
Use this space to note when a drift was found and how it was resolved —
useful history for understanding recurring patterns.

| Date | Item | Resolution (code fixed / doc updated) |
|---|---|---|
| 2026-08-04 | Checklist demanded Light theme support, contradicting Tier 1 dark-only | Doc updated — item inverted into a violation detector for Light/dynamic color |
| 2026-08-04 | Domain → Entity conversion had no defined home (write path) | Doc updated — `toEntity()` legitimized in CLAUDE.md + `new-mapper`; boundary rule restated as "DTO/Entity types never surface outside Data" |
| 2026-08-04 | `completedAt` transition rule fit in no layer without violating a rule | Doc updated — declared a persistence invariant owned by the Repository, as an explicit documented exception |
| 2026-08-04 | DESIGN.md asked for dynamic color; code and brand identity forbid it | Doc updated — code was right (`Theme.kt` never had it); DESIGN.md, Spec and CLAUDE.md now state the ban |
| 2026-08-04 | DESIGN.md prose specified white button labels; `on-primary` is #3c0091 | Doc updated — prose corrected to use the `on-*` role |
| 2026-08-04 | Skills referenced undefined concepts (Human-in-the-Loop, MVI contracts, Block 4, Self-Healing Loop, `discovery` skill) | Doc updated — all four defined in CLAUDE.md/Spec; the fifth reference removed |
| 2026-08-04 | Room/DAO, Hilt modules and interceptors had no owning Skill | Skills added — `new-room-dao`, `new-hilt-module` |
| 2026-08-04 | Type/Shape/unit mappings decided in code but recorded nowhere | Doc updated — DESIGN.md now carries both mapping tables and the rem→dp rule |
| 2026-08-05 | `Type.kt` implemented 7 of the 9 token→role rows DESIGN.md now defines — `titleMedium`/`titleSmall` were missing, so `title-md`/`title-sm` (the card-title styles) silently fell back to M3 defaults in Inter rather than Hanken Grotesk | Code fixed — both roles added with DESIGN.md's values |
| 2026-08-05 | `AuthRepositoryImpl.getValidAccessToken()` chained `runCatching{}.map{}.also{}` to perform the cache assignment — the exact shape CLAUDE.md → Code Conventions → Tier 3 names ("a chain performing a side effect, e.g. a cache assignment") | Code fixed — cache write extracted to a named `token` variable and a plain assignment |
| 2026-08-05 | Hilt visibility rule ("keep it private if nothing outside the module injects it") collided with the `@Singleton` rule: applied literally to `NetworkModule`'s OkHttp/Retrofit providers it would have created one client per consumer, since scope applies to bindings and not to functions | Doc updated — code was right; `new-hilt-module` and CLAUDE.md now carve out the exception and state the real dividing line (would a duplicate instance be harmful) |
| 2026-08-06 | `write-tests` and `new-mapper` claimed the write-path mapper round trip (`toEntity().toDomain()`) "proves TypeConverters line up" — false: those functions never call Room's registered `@TypeConverter`, so a wrong/unregistered converter would pass every existing test (found via Codex PR review on #2) | Doc updated — claim corrected in both Skills; added a direct, JVM-only `@TypeConverter` test (round trip + fallback) as its own coverage step in `new-room-dao` and `write-tests`, closing the actual gap instead of just disclosing it |
| 2026-08-06 | `new-hilt-module`'s "Current modules" table listed `DatabaseModule` as already existing; no `core/data/di/DatabaseModule.kt` exists in the repo (found via Codex PR review on #2) | Doc updated — marked `DatabaseModule (PENDING — not yet created)` per CLAUDE.md's Documentation completeness rule, with a note to create it on the first Room-backed binding |
| 2026-08-06 | Building the Search feature required deciding whether result cards show genre/developer per game — a product-level detail the Spec's one-line MVP scope for Search never recorded, though the approved mockup already showed it | Doc updated — Spec's Search section now states result cards show cover/name/genre/developer, sourced from IGDB `genres`/`involved_companies`, and distinguishes this from the still-backlog search filters |
| 2026-08-22 | Code review of PR #3 found six defects the `build`/`test`/`lint` gate passed over, three of them introduced by the very fixes made earlier that day: a dedup early-return that skipped cancelling a pending job (results for a superseded query painted over the current text), `isRefreshing` never reset when clearing the query mid-refresh, `errorMessage` outliving the next keystroke's debounce, "Retry" showing "No Results Found" during the retry request, a rendezvous `Channel` replaying a duplicated navigation effect on return, and a blank Search screen before any query | Code fixed with regression tests for each; the two that were also documentation gaps went both ways — CLAUDE.md's MVI section now states why the `Channel` queues, why `SharedFlow(replay = 0)` is deliberately not used, and that the invariant is one effect per user intent (`rememberSingleClick`); the Spec now specifies Search's initial state, which no mockup had ever defined |
| 2026-08-22 | `project-scaffold` was deleted from `.claude/skills/` while CLAUDE.md, the Spec, `new-feature` and `new-hilt-module` still referenced it as invocable | Doc updated — deletion kept (a once-per-project Skill only adds a dead option to the listing), content archived at `docs/project/retired-skills/project-scaffold.md`, CLAUDE.md gained a "Retired skills" entry, and the remaining references were reworded to past tense. Note for the record: the original motivation was token cost, which is largely a non-issue — Claude Code loads only a Skill's name and description up front, so an unused Skill costs about one line; the real justification is keeping dead options out of the listing |
| 2026-08-22 | The same rationale for `try`/`catch` over `runCatching` was written out in full in two RepositoryImpls, and the navigation structure rationale was duplicated between CLAUDE.md and code comments — the exact duplication CLAUDE.md → Tier 3 forbids, guaranteeing the copies drift | Both — doc updated and code fixed. The generalizable rule moved to CLAUDE.md (Architecture → Tier 1) and `new-repository-impl`; code comments shrank to one-line pointers. Rule of thumb recorded by example: a rule that applies to future code lives in CLAUDE.md plus its Skill and gets a pointer in code; rationale for one non-obvious line stays in the code; never both |
| 2026-08-22 | CLAUDE.md's Git workflow was the only section with no Tier markers, named a "build/tests/lint gate" it never defined, and said nothing about branching, untracked files, or PRs — gaps that produced a real near-miss (untracked `ApicalypseRequestBody.kt` would have been dropped by `git add -u`) and a whole feature left uncommitted | Doc updated — section rewritten with Tier 1/2/3, the gate defined as all three Gradle commands green, plus branch naming, Conventional Commits (adopted 2026-08-22; earlier commits deliberately not rewritten), a light PR rule, and an explicit "read `git status` in full" step |
| 2026-08-22 | Neither CLAUDE.md nor the Spec said how the bottom nav back stack works, so `Detail` had been implemented as a top-level sibling of the tabs. It got swept into the tabs' `saveState`/`restoreState` without belonging to any tab: leaving a tab from a Detail and returning restored the Detail under no selected tab, and the workaround for it used `NavController.currentBackStack` — `@RestrictTo` API that made `./gradlew lint` fail with a `RestrictedApi` **Error** | Both — code fixed and docs updated. Code: each tab is now a nested graph (`HomeGraph`/`SearchGraph`/`LibraryGraph`) with `Detail` registered inside every one via a shared `NavGraphBuilder` helper, restoring the canonical `popUpTo(findStartDestination){saveState}` + `restoreState` idiom and removing the restricted-API workaround. Docs: CLAUDE.md → Architecture now specifies the structure and every intended back-stack behavior; the Spec's App Navigation section states the user-facing half (a tab restores where you left it, Detail included). Decision approved by the human as a plan-level choice |
| 2026-08-22 | CLAUDE.md defined "done" as `build`/`test`/`lint` only, so a UI feature could be declared finished with no device check — and none was proposed until the human asked for one. Every UI defect this session (premature empty state, keyboard covering a button, clipped genre text) was invisible to that gate. The technique needed to run the check was also unrecorded: it lived only in one session's memory, where a fresh agent on another machine would have to rediscover it | Doc updated — CLAUDE.md → Commands gained a Tier 2 "Device verification" section: UI features are verified on the emulator before merge, the emulator is *asked for* rather than launched, and `Grep` over `uiautomator dump` versus `screencap` is chosen by which question is being asked, with the IME blind spot stated. `docs/project/design/README.md` created for the mockup-render recipe and the screenshot-cost model — deliberately not in CLAUDE.md, which governs architectural rules, not tool invocations. Also recorded there: shrinking the AVD saves no tokens, since the long edge is clamped before billing |
| 2026-08-22 | Tier 1's blanket "never force-push a branch that has already been pushed" made squash-merge the only remedy for an untidy branch, which contradicted the merge-strategy rule adopted the same day (merge commit by default, precisely to keep `blame` and `bisect` granularity). The absolute rule was guarding against overwriting a collaborator's work — a risk that does not exist in a solo repo | Doc updated after explicit conversation, as Tier 1 requires. Tier 1 keeps the absolute ban on force-pushing `main`, any branch another person may hold, and bare `--force` anywhere; Tier 2 now permits `--force-with-lease` on your own feature branch **until its first review**. The window is anchored to the first review rather than the first push because rewriting commits orphans review threads, and that history is what caught two defects the gate passed on PR #3. Recorded in the rule itself that it is deliberately conditional where an absolute rule would be easier to obey, and why that trade was accepted |
| 2026-08-22 | A second Codex review pass found two defects that the first pass's own fixes had introduced: unconditionally cancelling `searchJob` on every keystroke also killed the request already in flight for the same effective query (a trailing space during a first search discarded the IGDB response and dropped the screen to "No Results Found"), and `rememberSingleClick` had been applied to the result cards only, leaving the empty-state "Go To Library" CTA able to queue a duplicate navigation effect | Code fixed with two regression tests, both verified to fail without the fix. `SearchViewModel` now tracks `searchJobQuery` — the effective query the active job serves — because "a job is already searching this exact text, let it land" and "a job is scheduled for text the user abandoned, cancel it" reach the same branch and need opposite handling; `cancelSearch`/`startSearch` extracted so refresh and debounce share one path. CLAUDE.md's MVI section reworded: the guard applies to *every* navigation callback and is audited per screen, not per list — the list-centric wording is what caused the CTA to be missed. A no-argument `rememberSingleClick` overload added so single controls do not read as `rememberSingleClick<Unit>`. Note: the CTA fix has no unit test, since `rememberSingleClick` is a Composable and instrumented tests remain deferred |
| 2026-08-22 | The Git workflow said nothing about merge strategy, PR size, or how bot review threads get closed, and "one branch per unit of work" had no enforcement point — PR #3 reached 1,992 additions across 46 files with only two commits belonging to the Search feature, and a Codex review thread stayed open on a finding that had already been fixed | Doc updated — merge strategy stated (`Create a merge commit` default, `Squash and merge` as fallback, `Rebase and merge` banned, with the reasoning for each), a per-commit "does this exist because of this branch?" checkpoint and a 400-line/15-file smoke alarm added, bot threads must be answered then resolved, and `.github/pull_request_template.md` created so the screenshot drop point is signposted automatically. Recorded consequence of the existing Tier 1 force-push ban: history can only be cleaned before the first push, which is precisely why squash-merge has to exist as a fallback |
| 2026-08-22 | `docs/project/design/search/search-initial.png` was a hand-taken screenshot of its HTML, reproducible by nobody — window size, zoom and OS scaling were recorded nowhere | Asset fixed — regenerated headlessly at 600×1775 from the HTML beside it; `design/README.md` now carries the command so the next one is reproducible |
