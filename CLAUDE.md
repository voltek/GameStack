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

### Tier 2 — Configurable within a defined range
- Feature-based package structure (`feature/search/`, `feature/detail/`,
  `feature/library/`, `core/domain/`, `core/data/`) — the *principle* of
  feature-based organization is fixed, but exact folder names can evolve.
  Ask first: every Skill's "Location" section references these paths.
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
  expiration (with a safety buffer). An `AuthInterceptor` attaches the
  token to every IGDB request automatically — never fetch or attach it
  manually in a UseCase/ViewModel.
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

---

## Code Conventions

### Tier 1 — Immutable
- PascalCase for classes, camelCase for functions — Kotlin ecosystem-wide convention.
- Mappers must live in the data layer and return only Domain models — breaks the
  Domain/Data boundary if violated.
- All code and comments in English — foundational project decision.
- Never hardcode UI strings in ViewModels or UseCases — always use `UiText`
  (`UiText.StringResource` for translatable strings, `UiText.DynamicString`
  only for truly dynamic values from an API or database).

### Tier 2 — Configurable within a defined range
- One class per file, EXCEPT:
  (1) MVI contract classes (`UiState`, `UiEvent`, `UiEffect`) for the same
  screen may live together in a single file — they only make sense as a
  group and are always edited together.
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
  unnecessarily widening the DI graph.
- If a dependency is genuinely needed by multiple modules, extract it to
  its own appropriately-named module (e.g. `SerializationModule` for
  `Json`) — Hilt's graph is flat, not hierarchical, so any module can
  consume it regardless of which file provides it. Avoid a catch-all
  "CommonModule" — name modules by what they represent.

---

## Available Skills
This project uses Claude Code Skills for repeatable procedures. Prefer invoking
these over improvising the same task differently each time:

- `new-repository-interface` — Domain layer Repository contracts
- `new-repository-impl` — Data layer Repository implementation
- `new-api-service` — IGDB Retrofit interface (Apicalypse syntax)
- `new-mapper` — DTO/Entity → Domain conversion
- `new-usecase` — Domain layer UseCases
- `new-viewmodel` — MVI ViewModels
- `new-screen` — Composable screens
- `write-tests` — Unit tests for any of the above
- `new-feature` — orchestrates all of the above end-to-end

## Pending / Roadmap
- Block 5: AI-powered recommendations based on user history (Room).
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
