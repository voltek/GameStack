# CLAUDE.md — GameStack

## What is this project
GameStack is a native Android app for exploring video games using the IGDB API.
Users can search, view game details, manage personal lists, and rate games.
Not a store or gaming platform — a catalog and personal library app.

Full product scope, MVP feature list, and backlog live in
`docs/project/GameStack-Spec-v1.md` — consult it for product-level questions
(what should exist, what's deferred). This document (CLAUDE.md) governs
technical/architectural rules only.

## How to read this document — Three-Tier Boundary System
Every rule below belongs to one of three tiers:

- **Tier 1 — Immutable.** Breaking this rule either breaks the system mechanically,
  or contradicts a deliberate foundational decision. Never deviate without an
  explicit conversation first.
- **Tier 2 — Configurable within a defined range.** A bounded exception already
  exists and is listed. Anything outside that stated bound still requires asking first.
- **Tier 3 — Suggested.** Can be challenged with a stated justification. Propose
  the alternative and the reason — don't substitute silently.

## Documentation completeness rule
If this document (or a Skill) references a component that doesn't fully
exist or isn't fully specified yet, that reference must either be fully
specified in the same edit, or explicitly marked `(PENDING — not yet
defined)`. A vague reference that looks complete is worse than an honest
gap — it hides the missing piece until an agent trips over it mid-task.
(This is the exact pattern that happened with AuthRepository — referenced
in this document before its behavior was written down anywhere. Now fixed;
the rule exists to catch the next case, whatever it turns out to be.)

## Human-in-the-Loop principle

### Tier 1 — Immutable
Decisions split into two kinds, and they are handled differently:

- **Plan-level decisions** — anything requiring judgment that isn't mechanically
  derivable from this document, the Spec, or DESIGN.md: visual/UX choices,
  product scope, naming of a user-facing concept, or any trade-off with no
  single correct answer. **The agent proposes, the human decides.** Stop and
  wait for explicit approval; do not proceed on a "reasonable default".
- **Implement-level decisions** — anything already determined by the documents
  or by an existing pattern in the codebase. The agent proceeds without asking.

When a step is Plan-level, the Skill that owns it says so explicitly and names
the checkpoint (e.g. `new-feature` step 5, the screen prototype). Running such a
Skill in Claude Code's Plan Mode enforces the pause mechanically (read-only until
approved) rather than relying on the agent to honor it.

Related but distinct: the Tier system governs *rules* (what may be changed and
who must agree). This principle governs *steps in a procedure* (when to stop and
ask mid-task). A Tier 1 rule can only be changed by conversation; a Plan-level
step must be approved every single time it runs.

## Self-Healing Loop

### Tier 1 — Immutable
When code and documentation disagree, that mismatch is **drift**, and drift is
never resolved silently. Resolve it in one of exactly two directions, and state
which one was chosen:

1. **Fix the code** — the document was right; the code deviated.
2. **Fix the document** — the code turned out to be right and the document is
   what's outdated (e.g. an implementation decision was made that the document
   never recorded).

Either way, log it in `docs/project/DRIFT-CHECKLIST.md`'s Resolution log.
Never leave a known contradiction unresolved on the grounds that "the code works" —
an undocumented decision is invisible to the next agent, which is how the same
drift returns.

## Commands
- Build: `./gradlew build`
- Unit tests: `./gradlew test`
- Lint: `./gradlew lint`

## Git workflow
- Commit at logical checkpoints — after a Skill completes and the build is
  verified green (e.g. after `project-scaffold`, after a feature's full
  `new-feature` flow, after a standalone piece like AuthRepository).
  Use a short, descriptive message (e.g. "Add AuthRepository with token
  caching and auto-refresh").
- `git commit` freely once the build/tests/lint gate passes.
- Always ask before `git push` — commits stay local and reversible; push
  goes to the remote and is less easily undone.

---

## Tech Stack

### Tier 1 — Immutable
- Kotlin (latest stable) — project language.
- Jetpack Compose — all UI, no XML, no ViewBinding.
- MVI + Clean Architecture — governs every structural rule in this document.
- Retrofit + Kotlinx.serialization — networking (see Data Sources for the
  Apicalypse-specific constraint).
- Navigation Compose with Safe-type routes.
- Material3 — Dark theme only for MVP. Light theme is backlog (see
  Pending/Roadmap) — do not build a Light ColorScheme now.
- **No dynamic color (Material You wallpaper-based theming).** The palette is
  the fixed one in DESIGN.md. Dynamic color would let the OS override the
  violet that carries the "Stack" brand identity, and would make DESIGN.md
  non-authoritative for what actually renders. `GameStackTheme` therefore takes
  no `dynamicColor` parameter and calls no `dynamicDarkColorScheme()`.
- Before adding ANY new library (not just replacing one below): check for the
  latest stable version first — never assume a remembered version is current.

### Tier 2 — Configurable within a defined range
- Room — local persistence. Swappable in theory (e.g. a future SQLDelight
  migration for KMP), but touches the Data layer contract broadly — ask first.
- Hilt — dependency injection. Same reasoning as Room: high blast radius, ask first.

### Tier 3 — Suggested, can be challenged with justification
- Coil — image loading. If there's a concrete reason (deprecation, critical bug,
  better Compose support elsewhere), propose an alternative and explain why.

---

## Testing Stack

### Tier 1 — Immutable
- Turbine, MockK, TestDispatcher — baked into the `write-tests` skill's patterns.
  Changing any of these breaks consistency across every test in the project.
- Use the project's `MainDispatcherRule` (`core/testing/`) for ViewModel tests
  instead of manually managing `Dispatchers.setMain()`/`resetMain()`.
- **Everything under `test/` is a plain JVM unit test** — no Robolectric, no
  emulator, no real database or network. `./gradlew test` is the gate, and it
  must stay fast enough to run on every change. Anything genuinely requiring
  the Android runtime (a real Room DAO against SQLite, Compose UI interaction)
  is an *instrumented* test under `androidTest/` — currently backlog, not MVP.
  Consequence: DAO interfaces are verified indirectly, through Repository tests
  with a mocked DAO. See `write-tests` for what that does and does not prove.

See the `write-tests` skill for full conventions (setup per layer, naming, Happy/Sad paths).

---

## Architecture

MVI with Clean Architecture. Three logical layers (UI, Domain, Data) as packages
within a single app module.

### Tier 1 — Immutable
- UI never accesses the Repository directly — always via UseCase through the ViewModel.
- Repositories contain no business logic — they only coordinate data sources.
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

### MVI Contract conventions (Tier 1)
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

All three live together in `feature/{name}/presentation/{ScreenName}Contract.kt`
— this is the one-class-per-file exception (1) below. The ViewModel exposes a
single public entry point, `fun handleEvent(event: UiEvent)`.

### Tier 2 — Configurable within a defined range
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

### Tier 1 — Immutable
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

### Tier 2 — Configurable within a defined range
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

  This is deliberately *not* a UseCase's job: a UseCase would have to read the
  current entity to compare, and entities never leave the Data layer. It is
  deliberately not the DAO's job either, since it spans a read and a write.
  The boundary that still holds: the Repository decides *when a timestamp
  column is written*, never *what the timestamps mean to the user* — anything
  user-facing (formatting "Completed on [date]", ordering "Recently Interacted")
  stays in the Domain/UI layers. If a future rule needs more than comparing
  old and new persisted state, that is real business logic and belongs in a
  UseCase — ask first.

---

## Code Conventions

### Tier 1 — Immutable
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

### Tier 2 — Configurable within a defined range
- One class per file, EXCEPT:
  (1) MVI contract classes (`UiState`, `UiEvent`, `UiEffect`) for the same
  screen live together in `{ScreenName}Contract.kt` — they only make sense as a
  group and are always edited together. This is the expected layout, not merely
  a permitted one; splitting them across three files is what needs justifying.
  (2) A small `private` helper type used exclusively by one Composable/class
  in that same file (e.g. a private data class configuring a static list of
  items for that Composable) — it has no meaning or reuse outside that context.

### Tier 3 — Suggested, can be challenged with justification
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

---

## Available Skills
This project uses Claude Code Skills for repeatable procedures. Prefer invoking
these over improvising the same task differently each time:

**Setup / planning**
- `project-scaffold` — bootstraps an empty project (deps, folders, Hilt, Theme,
  test utilities). Runs ONCE per project; already done for GameStack.
- `discovery-feature` — turns a vague feature idea into a Spec section. Only
  needed when scope is genuinely ambiguous; the MVP screens are already specced.

**Implementation (in dependency order)**
- `new-repository-interface` — Domain layer Repository contracts
- `new-repository-impl` — Data layer Repository implementation
- `new-api-service` — IGDB Retrofit interface (Apicalypse syntax)
- `new-room-dao` — Entity + DAO + TypeConverters + Database registration
- `new-hilt-module` — Hilt bindings for anything the above creates
- `new-mapper` — `toDomain()` / `toEntity()` conversions
- `new-usecase` — Domain layer UseCases
- `new-viewmodel` — MVI ViewModels
- `new-screen` — Composable screens
- `write-tests` — Unit tests for any of the above
- `new-feature` — orchestrates all of the above end-to-end

If a task doesn't map to any Skill above, that's a coverage gap worth naming:
do the work, then say so, so the gap can be closed deliberately rather than
by improvising the same task differently every time.

## Pending / Roadmap

**About "Block N":** the numbering comes from the development phases this
project is being built in, and several documents reference it. Only the phases
actually referenced somewhere are defined — here and, in product terms, in the
Spec's "Roadmap Phases" section. Do not infer meaning for an undefined block
number; if a document references one that isn't defined in either place, that
is a documentation gap to report, not to guess at.

- **Block 4 — Loop Engineering** (harness work, not product): automate what is
  currently manual review. Two concrete deliverables are already referenced by
  other documents and both are still pending:
  1. A Hook that runs the affected test suite automatically after each agent
     edit — the Regression pillar that `write-tests` explicitly does not cover
     (it covers Requirements only: "new code does what the Skill/Spec asked").
  2. An automated drift checker (Hook or validator subagent) implementing
     `DRIFT-CHECKLIST.md`'s items, replacing the manual pass.
- **Block 5 — AI-powered recommendations** based on user history (Room).
- Future: KMP migration. Data layer prepared for it — Retrofit → Ktor,
  Room → SQLDelight, without touching Domain or UI.
- Future (backlog, not MVP): extend `GameRating` from binary (LIKED/DISLIKED)
  to a third "LOVED" state (Netflix/Prime-style double thumbs up).
  Trivial to add later since it's an enum — do not build now.
- Future (backlog, tooling): consider adopting OpenSpec as an active
  change-tracking layer alongside CLAUDE.md, IF the project grows beyond
  solo development (team context) or manual tracking via Skills becomes
  a real pain point. Not worth the added infrastructure at current scale.
- Future (backlog): Light theme ColorScheme. Project design identity
  (established via Stitch prototyping and GameStack Core DESIGN.md) is
  dark-first, matching gaming apps like Steam/Xbox/Discord. Not worth
  building a parallel Light ColorScheme now — revisit only if there's a
  real accessibility or user need.
