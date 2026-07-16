# CLAUDE.md — GameStack

## What is this project
GameStack is a native Android app for exploring video games using the IGDB API.
Users can search, view game details, manage personal lists, and rate games.
Not a store or gaming platform — a catalog and personal library app.

## Tech Stack
Kotlin (latest stable), Jetpack Compose, MVI + Clean Architecture,
Retrofit + Kotlinx.serialization, Room, Hilt, Coil, Navigation Compose (Safe-type),
Material3 (Dark/Light themes required).

## Testing Stack
Turbine, MockK, TestDispatcher. See the `write-tests` skill for conventions.

## Commands
- Build: `./gradlew build`
- Unit tests: `./gradlew test`
- Lint: `./gradlew lint`

## Architecture
MVI with Clean Architecture. Three logical layers (UI, Domain, Data) as packages
within a single app module, organized by feature:
`feature/search/`, `feature/detail/`, `feature/library/`, `core/domain/`, `core/data/`

- **UI:** Composables (view logic only) + ViewModels (MVI pattern — see `new-viewmodel`
  and `new-screen` skills).
- **Domain:** Use Cases and pure models. Only allowed external dependency: `javax.inject` (Hilt).
- **Data:** Repositories, DAOs, API Services, Mappers.

## Rules that are never broken
- UI never accesses the Repository directly — always via UseCase through the ViewModel.
- Repositories contain no business logic — they only coordinate data sources.
- Domain models never import Retrofit, Room, or Android types.
- Never use LiveData — only StateFlow and Flow.
- Never hardcode UI strings — always use `UiText`.
- Always check for the latest stable version before adding a new library.
- Never make architectural decisions without asking first.

## Data Sources
- **IGDB API:** read-only. Requires OAuth via Twitch — see AuthRepository.
  IMPORTANT: IGDB does not use standard REST. It uses Apicalypse — every request
  is a POST with the query as plain text in the request body, not GET with query
  params. See the `new-api-service` skill before touching the API layer.
- **Room (local):** a single `UserGameEntity` per game the user interacted with —
  `gameId` (PK), `listStatus` (nullable: PLAYING_NOW/WANT_TO_PLAY/COMPLETED),
  `rating` (nullable: `GameRating.LIKED`/`DISLIKED`), `updatedAt`. Do not split
  lists and ratings into separate tables — a game can have either, both, or
  neither independently. Never synced with IGDB's own community rating.

## Code Conventions
- File names: PascalCase for classes, camelCase for functions.
- One class per file.
- Mappers: `{DtoOrEntity}Mapper.kt`, always in the data layer.
- All code and comments in English.

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
