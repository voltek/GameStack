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

## Home Screen
- Most popular games today
- Games the user has recently interacted with
- Quick access to the user's personal lists
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
- Material3 (Material You) — Dark and Light themes required

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

## Related Documents
- `CLAUDE.md` — technical/architectural rules for AI agents working on this repo
- `.claude/skills/` — reusable procedures (new-feature, new-mapper,
  new-repository-interface, new-repository-impl, new-api-service,
  new-usecase, new-viewmodel, new-screen, write-tests)
