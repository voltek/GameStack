# CLAUDE.md — GameStack

## What is this project
GameStack is a native Android app for exploring video games via the IGDB API:
search, game details, personal lists and ratings. A catalog and personal library,
not a store or gaming platform. Scope and what is deferred: the PRD
(`docs/project/PRODUCT.md`). This document governs technical rules.

## Purpose
Half product, half a harness meant for other projects, and both a case study
(PRD → *Why this project exists*). Weigh every proposal and decision against
both halves and say what it gives each. A harness decision lives where a clone
reads it — an ADR, TOOLING.md, a Skill — never only outside the repo. Governance
that serves neither the next product step nor what another project would take
is a cost.

## Working here
- Before committing, run `./gradlew build`, `./gradlew test` and `./gradlew lint`;
  all three must be green. A lint Error fails the gate, a warning does not.
- Put code and its documentation in the same commit.
- Before pushing, `git fetch` and reconcile with `origin/main`.
- One branch per unit of work, checked per commit: does this exist because of this
  branch? Anything else goes on its own branch.
- Push your own feature branch without asking. Ask before every PR action GitHub
  shows.
- Never commit on `main`: branch first.
- Never use `--no-verify`, never force-push `main` or a branch another person may
  hold, and never use bare `--force`, which overwrites the remote without checking.
  `--force-with-lease` may clean your own feature branch until it merges.
- Never change branch protection or repository settings.
- Update a pushed branch with `git merge main`, never `git rebase`.
- Touching the same code a third time for the same class of defect means the design
  is the problem: stop patching and redesign.
- Ask first: any decision the documents or an existing code pattern do not settle,
  such as a visual or UX choice, product scope, the name of a user-facing concept, or a trade-off with no
  single answer. Propose it and wait for approval, even when the request looks small.
- When code and a document disagree, say which is right and why, and put
  `Drift: code fixed`, `Drift: doc updated` or `Drift: both` on the commit that
  resolves it. A rule that is born or changes is a new ADR.
- A rule changes only with the owner's agreement, through an ADR. A rule under a
  "Prefer" heading may be challenged with a stated reason.
- Run the `ship-a-branch` Skill to take a finished branch to a merged PR.

## Tech Stack

- Kotlin (latest stable) — project language.
- Jetpack Compose — all UI, no XML, no ViewBinding.
- MVI + Clean Architecture — governs every structural rule in this document.
- Retrofit + Kotlinx.serialization — networking (see Data Sources for the
  Apicalypse-specific constraint).
- Navigation Compose with Safe-type routes.
- Material3 — Dark theme only for MVP. Light theme is backlog (see the PRD) — do not build a Light ColorScheme now.
- **No dynamic color (Material You wallpaper-based theming).** The palette is
  the fixed one in DESIGN.md. Dynamic color would let the OS override the
  violet that carries the "Stack" brand identity, and would make DESIGN.md
  non-authoritative for what actually renders. `GameStackTheme` therefore takes
  no `dynamicColor` parameter and calls no `dynamicDarkColorScheme()`.
- Before adding ANY new library (not just replacing one below): check for the
  latest stable version first — never assume a remembered version is current.
  **Read `maven-metadata.xml` and sort the versions**; Maven Central's search
  endpoint does not order by version, so its first page can look like the latest
  and not be. That mistake shipped a two-releases-old Robolectric past a review
  of this very rule.

- Room — local persistence. Swappable in theory (e.g. a future SQLDelight
  migration for KMP), but touches the Data layer contract broadly — ask first.
- Hilt — dependency injection. Same reasoning as Room: high blast radius, ask first.

### Prefer
- Coil — image loading. If there's a concrete reason (deprecation, critical bug,
  better Compose support elsewhere), propose an alternative and explain why.

---

## Architecture

MVI with Clean Architecture. Three logical layers (UI, Domain, Data) as packages
within a single app module.

- UI never accesses the Repository directly — always via UseCase through the ViewModel.
- Repositories contain no business logic — they only coordinate data sources.
- **Never build a `Result` with `runCatching` around a suspend call.** It catches
  `Throwable`, `CancellationException` included, so a coroutine that was merely
  cancelled (a superseded debounce, a ViewModel clearing) comes back as
  `Result.failure` and surfaces to the user as a real error. Use `try`/`catch`
  that rethrows `CancellationException` before catching `Exception`. This is not
  a style preference: it produced a visible bug where every cancelled Search
  flashed the error screen before the next results landed.
- Domain models never import Retrofit, Room, or Android types (only allowed
  external dependency: `javax.inject` for Hilt).
- Never use LiveData — only StateFlow and Flow.
- Composables hold no logic except view logic.
- **UI layer:** Composables + ViewModels (MVI pattern — see `new-viewmodel` and `new-screen` skills).
- **Domain layer:** UseCases and pure models.
- **Data layer:** Repositories, DAOs, API Services, Mappers.
- **Navigation:** persistent bottom navigation bar with exactly 3 top-level
  destinations — Home, Search, Library — following Material 3 guidelines
  (3-5 destinations of equal importance, reachable from anywhere). Detail is
  NOT a bottom nav destination — it's reached from a game card in any of
  the three, not a top-level tab. High cost to change once built — ask first.
- **Navigation structure — one nested graph per tab.** Each tab is a nested
  graph (`HomeGraph`/`SearchGraph`/`LibraryGraph`) whose start destination is
  that tab's screen, and `Detail` is registered *inside every tab graph* via the
  shared `NavGraphBuilder.gameDetailDestination()` helper — never once at top
  level. That is what gives each tab its own back stack. It does not add a
  fourth tab: the graphs are structure, the tabs are still exactly three.
  Consequences, all intended:
  - A Detail belongs to the tab that opened it: that tab stays selected while
    Detail is on screen, and Back from Detail returns to that tab's own screen.
  - Switching tabs saves the originating tab's whole stack (Detail included) and
    restores it on return — coming back to a tab lands you exactly where you
    left it, **including on an open Detail**. Returning to a tab deliberately
    does not reset it to its root.
  - Back from a non-start tab goes to Home, then exits the app. This is standard
    Android bottom nav behavior, not a defect — do not "fix" it.
  - Bottom nav items and `navigateToBottomNavDestination()` always target a
    `*Graph` route, never the screen inside it; targeting the screen stops the
    tab's back stack from being the unit that gets saved and restored.
  - Never use `NavController.currentBackStack` — it is `@RestrictTo`
    library-group API and fails the `lint` gate. If tab switching appears to
    need it, the graph structure is wrong, not the API.

  Registering `Detail` once at top level instead is what previously let it leak
  across tabs — it would reappear under an unrelated tab with no tab selected.

### MVI Contract conventions
Every screen defines exactly three contract types, and they are the only way
UI and ViewModel communicate:

- `{ScreenName}UiState` — a data class, exposed as `StateFlow`, backed by a
  private `MutableStateFlow`. Use a sealed class only when the states are
  genuinely mutually exclusive; a data class with nullable/boolean fields is
  the default, because loading, content, and error frequently coexist (e.g.
  a refresh failing while stale content is still on screen).
- `{ScreenName}UiEvent` — a sealed class covering every action the UI can send.
- `{ScreenName}UiEffect` — a sealed class of one-shot events (navigation,
  snackbars), exposed via `Channel` + `receiveAsFlow()`. Never put a one-shot
  event in UiState: state replays on recomposition/rotation, so a navigation
  or snackbar modeled as state fires twice.
  The `Channel` **queues** anything sent while the screen is not collecting —
  that is deliberate (a snackbar raised during a config change must survive),
  and it is why the alternative, `SharedFlow(replay = 0)`, is not used here:
  it would silently drop those instead. The cost of queueing is that a
  *duplicated* effect is not merely handled twice, it is replayed when the user
  returns to the screen. So the invariant is at the source: **one user intent
  must emit exactly one effect.** Wrap **every** callback that emits a
  navigation effect in `rememberSingleClick` (`core/presentation/`) — a list's
  items, an empty-state CTA, a toolbar action, all of them. For a list, share one
  wrapper across the whole list so two different items cannot fire back to back;
  a no-argument overload exists for single controls. Auditing this is per screen,
  not per list: the CTA on Search kept emitting duplicates because the wrapper
  was introduced for the result cards only, which is where the bug had been seen.
  Do not "fix" a duplicated effect by changing the Channel — that trades a
  visible bug for a silent one.

All three live together in `feature/{name}/presentation/{ScreenName}Contract.kt`
— this is the one-class-per-file exception (1) below. The ViewModel exposes a
single public entry point, `fun handleEvent(event: UiEvent)`.

### Packages and lists
- Feature-based package structure — the *principle* of feature-based
  organization is fixed, but exact folder names can evolve. Ask first: every
  Skill's "Location" section references these paths. The full set:
  - `feature/home/`, `feature/search/`, `feature/library/`, `feature/detail/`
    — one per screen, each with its own `domain/`, `data/`, `presentation/`
    subpackages as needed.
  - `core/domain/`, `core/data/` — shared across two or more features.
  - `core/presentation/` — shared UI-layer utilities that are not screens
    (e.g. `UiText`).
  - `core/testing/` — shared test utilities (e.g. `MainDispatcherRule`),
    in the **test** source set, not main.
  - `ui/theme/` — Theme, Color, Type, Shape. App-wide, not feature-scoped.
  - `navigation/` — NavHost, type-safe `Destination` routes, bottom nav bar.
- Pull-to-refresh is expected on any screen displaying a list/collection
  (Home, Search results, Library). Single-item screens (e.g. Detail) don't
  need it. See the `new-screen` skill for the implementation pattern.

---

## Data Sources

- **IGDB API:** read-only. Requires OAuth via Twitch — see AuthRepository.
  `AuthRepository` uses Twitch's Client Credentials flow (app-level auth,
  no user login involved) — a POST to `id.twitch.tv/oauth2/token` with
  client ID/secret. It caches the token in memory, guarded by a `Mutex`
  to prevent duplicate concurrent fetches, and auto-refreshes it before
  expiration (with a safety buffer). An `AuthInterceptor` attaches **both**
  required headers — `Client-ID` and `Authorization: Bearer {token}` — to
  every IGDB request automatically. Never fetch or attach them manually:
  not in a UseCase/ViewModel, and not as `@Header` parameters on the
  Retrofit interface either (see `new-api-service`).
  If the token cannot be obtained, the interceptor throws `IOException`
  and the request fails — it does **not** proceed unauthenticated. An
  unauthenticated IGDB call would fail at the server with a 401 anyway;
  failing early keeps the error at one layer instead of two. Retrofit
  surfaces this to the Repository as a normal network failure, so it flows
  into the existing `Result` error path with no special handling.
  The Twitch auth client is deliberately built **without** this interceptor —
  the call that fetches the token cannot depend on already having one.
  IMPORTANT: IGDB's own data endpoints (games, genres, etc.) do NOT use
  standard REST — they use Apicalypse: every request is a POST with the
  query as plain text in the body, not GET with query params. See the
  `new-api-service` skill before touching that layer. This does NOT apply
  to the Twitch auth call above, which uses standard REST.
- Personal ratings and lists are never synced with IGDB's own community rating —
  they are independent, local-only data.

- **Room (local):** a single `UserGameEntity` per game the user interacted with —
  `gameId` (PK), `listStatus` (nullable: PLAYING_NOW/WANT_TO_PLAY/COMPLETED),
  `rating` (nullable: `GameRating.LIKED`/`DISLIKED`), `updatedAt` (any change —
  powers "Recently Interacted" on Home), `completedAt` (nullable — set ONLY
  when `listStatus` transitions to COMPLETED, never touched by rating changes;
  powers "Completed on [date]" in Library). Do not conflate `updatedAt` and
  `completedAt` — they answer different questions. Do not split lists and
  ratings into separate tables — a game can have either, both, or neither
  independently. Never synced with IGDB's own community rating.

- **Who owns the `updatedAt`/`completedAt` write rules: the Repository.**
  Both are *persistence invariants* — statements about how a row is written —
  not domain rules, and they are explicitly permitted inside `UserGameRepositoryImpl`
  despite the "Repositories contain no business logic" rule. The mechanics:
  every write is read-modify-write (load the existing row, produce the updated
  copy, upsert), `updatedAt` is stamped on every write, and `completedAt` is
  stamped only when the incoming `listStatus` is COMPLETED and the stored one
  was not. Detecting that transition requires comparing against the stored row,
  which only the Repository can see.

  The boundary that still holds: the Repository decides *when* a timestamp column
  is written, never *what the timestamps mean to the user* — formatting and
  ordering stay in Domain/UI. A rule needing more than a comparison of old and
  new persisted state is real business logic and belongs in a UseCase — ask
  first. Why not a UseCase or the DAO: see `new-repository-impl`.

---

## Code Conventions

- PascalCase for classes, camelCase for functions — Kotlin ecosystem-wide convention.
- Mappers must live in the data layer. The rule they enforce is directional:
  **a DTO or Entity type must never appear in a signature outside the data layer.**
  Two mapping directions exist, and both are legitimate:
  - **Read path — `DtoOrEntity.toDomain()`**: converts inbound data into a Domain
    model. This is the common case and the only one `new-mapper` used to cover.
  - **Write path — `DomainModel.toEntity()`**: converts a Domain model into a Room
    Entity so it can be persisted. Its *input* is a Domain model and its *output*
    never leaves the Data layer, so the boundary holds. Needed by every write
    feature (saving a rating, moving a game between lists) — without it, that
    conversion would have no defined home.

  What remains forbidden: a mapper that returns a DTO/Entity to a caller in the
  Domain or UI layer, and any DTO→Entity mapper (remote and local models must not
  know about each other — convert through the Domain model). Same-layer
  transformations (building an updated copy of an Entity to persist) are not
  Mappers at all — see `new-repository-impl`.
- All code and comments in English — foundational project decision.
- Never hardcode UI strings in ViewModels or UseCases — always use `UiText`
  (`UiText.StringResource` for translatable strings, `UiText.DynamicString`
  only for truly dynamic values from an API or database).

- One class per file, EXCEPT:
  (1) MVI contract classes (`UiState`, `UiEvent`, `UiEffect`) for the same
  screen live together in `{ScreenName}Contract.kt` — they only make sense as a
  group and are always edited together. This is the expected layout, not merely
  a permitted one; splitting them across three files is what needs justifying.
  (2) A small `private` helper type used exclusively by one Composable/class
  in that same file (e.g. a private data class configuring a static list of
  items for that Composable) — it has no meaning or reuse outside that context.

- **Comments carry the *why*, never the *what* and never the *history*.** Three
  questions before leaving one:
  1. Does it restate what the code already says? Delete it.
  2. Is it about *how we got here* rather than *what to do here*? That belongs in
     the commit message. Writing it in both places is duplication, and the copy
     in the code is the one nobody updates.
  3. Would it become false if someone refactored correctly? Then it is a
     chronicle, not a constraint — delete it or rewrite it as the constraint.

  Prefer encoding the *why* as a name, a type, or a **test**: a test named
  `should cancel an in-flight search immediately when the query changes` states
  the invariant *and fails when it breaks*, which no comment can do.

  **Bound — a limit:** at most 4 lines per comment block, next to the line it
  constrains, in every hand-written file except documentation: code (main and
  test), build scripts and configuration, workflows, hooks and scripts.
  Generated or template text is exempt. The warning stays inline; a reason that
  needs more goes to a test that guards it, the ADR, TOOLING.md for a workflow
  or the owning Skill for a procedure, named in a one-line pointer, or to the
  commit body, which `git blame` on that line finds (ADR 0037). **15% comment
  lines in a file** is only a smoke alarm, meaningless below ~50 lines. Models
  narrate design decisions into comments, so the questions apply to every line.

### Prefer
- Mapper file naming pattern (`{Name}Mapper.kt`) — consistency preference,
  not a structural requirement.
- Avoid chaining more than 2 functional operators (`map`/`also`/`let`/etc.)
  in a single expression when the chain performs a side effect (e.g. a
  cache assignment) — prefer a named intermediate variable. Token cost of
  this is negligible; readability during future debugging matters more.
- Avoid duplicating non-trivial code (more than a couple of lines, or logic
  that could drift out of sync if only one copy gets updated) anywhere in
  the codebase — extract to a `private` function or named variable instead.
  Applies everywhere: ViewModels, UseCases, Repositories, Composables, Hilt
  modules, wherever the pattern appears — not scoped to any single layer.
- In Hilt modules specifically, beyond the duplication rule above: only
  expose a value via `@Provides`/`@Binds` if something outside this module
  genuinely needs to inject it directly; otherwise keep it private to avoid
  unnecessarily widening the DI graph. Exception: anything that must be
  `@Singleton` stays a binding even if only same-module consumers inject it —
  scope applies to bindings, not to functions, so a private helper would
  silently create one instance per call site. See `new-hilt-module`.
- If a dependency is genuinely needed by multiple modules, extract it to
  its own appropriately-named module (e.g. `SerializationModule` for
  `Json`) — Hilt's graph is flat, not hierarchical, so any module can
  consume it regardless of which file provides it. Avoid a catch-all
  "CommonModule" — name modules by what they represent.
