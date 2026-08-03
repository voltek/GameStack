# GameStack — Spec v1.0

## What it is
GameStack is a native Android app for exploring video games and managing a
personal game library, using the IGDB API as the data source. Users can
search for games, view detailed info, organize personal lists, and rate
games they've played.

It is NOT:
- A store (purchase links redirect externally, no in-app purchases)
- A gaming platform (no gameplay inside the app)
- A social/gameplay video platform (only official trailers are shown,
  no user-generated gameplay videos)

## Core Features (MVP)

### Search
- Search games by name via the IGDB API.

### Game Detail
- Name, description
- IGDB community rating
- Official trailer
- External purchase links (when available from IGDB)

### Personal Lists
Three lists, managed locally in Room:
- Playing Now
- Want to Play
- Completed

### Personal Rating
- Thumbs up / thumbs down (`GameRating.LIKED` / `GameRating.DISLIKED`)
- Nullable — a game can be unrated
- Stored locally in Room, never synced with IGDB's own community rating
- Backlog (not MVP): possible future third state "LOVED" (double thumbs up,
  Netflix/Prime-style). Trivial to add later since it's an enum.

## App Navigation
Persistent bottom navigation bar with 3 top-level destinations:
- **Home** — discovery: popular games + recently interacted games
- **Search** — dedicated search screen
- **Library** — the user's 3 personal lists (Playing Now, Want to Play, Completed)

Game Detail is not a top-level destination — reached by tapping a game card
from any of the three tabs above.

## Home Screen
- Most popular games today
- Games the user has recently interacted with
- (Optional / deferred to Block 5) AI-based recommendations based on
  user preferences and rating history

## Tech Stack
- Kotlin (latest stable)
- Jetpack Compose — all UI, no XML
- MVI + Clean Architecture
- Retrofit + Kotlinx.serialization — networking
- Room — local persistence
- Hilt — dependency injection
- Coil — image loading
- Navigation Compose (Safe-type routes)
- Material3 (Material You) — Dark theme only for MVP (see Explicitly Deferred)

## Data Sources
- **IGDB API** (read-only): game info, images, trailers, community rating.
  Requires OAuth authentication via Twitch.
  IMPORTANT: IGDB uses Apicalypse, not standard REST — requests are POST
  with the query as plain text in the request body, not GET with query params.
- **Room** (local, read/write): user lists and personal ratings.
  Never synced back to IGDB.

## Explicitly Deferred
- **Block 5:** AI-powered game recommendations based on user history (Room data).
- **Future:** KMP migration. The Data layer is architected for it —
  when the time comes, Retrofit → Ktor and Room → SQLDelight,
  without touching Domain or UI.
- **Backlog:** third rating state ("LOVED").
- **Backlog:** recent search history on the Search screen (would require new
  local storage for query strings — DataStore, not Room). Search works fully
  without it; deferred to keep MVP scope tight.
- **Backlog:** quick-add to list directly from Home/Search game cards (3-dot
  menu → list picker), without entering Game Detail first. Low cost (reuses
  existing UserGameRepository logic) but adds UI surface not needed for MVP.
- **Backlog:** user profile (avatar photo + display name), stored locally only
  (Room/DataStore) — no IGDB dependency, purely cosmetic personalization.
  Surfaced during Stitch prototyping (top-right avatar icon in Home mockup),
  not part of MVP scope.
- **Backlog:** search filters (genre, platform, etc.) on the Search screen,
  with a "Clear all filters" action. Surfaced during Stitch prototyping of
  the empty search state — MVP search is plain text query only, no filters.
- **Backlog:** in-library search bar (search within the user's own saved
  games, not IGDB). Low cost — operates on already-fetched local Room data.
  Surfaced during Stitch prototyping of the Library screen.
- **Backlog (major architectural decision, not a small addition):** true
  offline mode — caching IGDB responses locally so the app remains usable
  without connectivity. Surfaced during Stitch prototyping (Error State
  originally included a "Go to Offline Mode" link). Unlike other backlog
  items, this is NOT low-cost: it requires a caching/sync strategy for
  remote data (separate from the existing Room tables for lists/ratings),
  decisions on cache invalidation and freshness, and likely a dedicated
  Repository-layer redesign. Treat as its own future evaluation — comparable
  in scope to the Retrofit-vs-Ktor decision — not something to bolt on casually.
- **Backlog:** Light theme. MVP ships dark-only — the project's visual
  identity (confirmed via Stitch prototyping and the GameStack Core
  DESIGN.md) is dark-first, matching genre conventions (Steam, Xbox app,
  Discord). Revisit if a real accessibility/user need arises.

## Related Documents
- `CLAUDE.md` — technical/architectural rules for AI agents working on this repo
- `.claude/skills/` — reusable procedures (project-scaffold, discovery-feature,
  new-feature, new-mapper, new-repository-interface, new-repository-impl,
  new-api-service, new-usecase, new-viewmodel, new-screen, write-tests)
- `docs/project/DESIGN.md` — visual design system (colors, typography, spacing)
- `docs/project/DRIFT-CHECKLIST.md` — manual audit checklist against CLAUDE.md Tier 1 rules