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
- Each result shows the game's cover, name, primary genre, and developer
  (IGDB's `genres` and `involved_companies` fields) — confirmed as part of
  building the Search feature, matching the approved `docs/project/design/search/`
  mockup. Distinct from search filters (letting the user filter results by
  genre/platform), which remains backlog — see Explicitly Deferred.

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

Each tab remembers where you were. Opening a Game Detail from a tab keeps that
tab selected, and leaving the tab and coming back returns you to exactly where
you left off — including to an open Game Detail, not the tab's starting screen.
Back from a Game Detail returns to the list it was opened from; Back from a
non-Home tab returns to Home.

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
- Material3 — Dark theme only for MVP (see Explicitly Deferred). No dynamic
  color: the palette is fixed in DESIGN.md so the brand violet always wins.

## Data Sources
- **IGDB API** (read-only): game info, images, trailers, community rating.
  Requires OAuth authentication via Twitch.
  IMPORTANT: IGDB uses Apicalypse, not standard REST — requests are POST
  with the query as plain text in the request body, not GET with query params.
- **Room** (local, read/write): user lists and personal ratings.
  Never synced back to IGDB.

## Roadmap Phases ("Block N")
Several documents reference development phases by number. Only the phases
actually referenced somewhere are defined — do not infer meaning for any other
block number; an undefined reference is a documentation gap to report, not to
guess at.

- **Block 4 — Loop Engineering.** Harness/tooling work, not product: automating
  what is currently a manual review pass. Two pending deliverables, both already
  referenced elsewhere: (1) a Hook running the affected test suite after each
  agent edit — the regression pillar `write-tests` explicitly does not cover;
  (2) an automated drift checker implementing `DRIFT-CHECKLIST.md`.
- **Block 5 — AI-powered recommendations.** Product feature; see below.

## Explicitly Deferred
- **Block 5:** AI-powered game recommendations based on user history (Room data).
- **Instrumented tests (`androidTest/`).** Everything currently tested runs on
  the JVM. Real-SQLite DAO tests, Room migration tests, and Compose UI tests need
  a device/emulator and are deferred. Consequence to keep in mind: a wrong `@Query`
  or a missing migration passes every existing test and fails on device. Revisit
  before the first release that has real users with data to lose.
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
  **Evaluate an explicit search/apply button together with this**, not before:
  results currently arrive on their own via the debounce, so a search button
  today would be a false affordance (tapping it changes nothing visible), and
  the keyboard's IME "Search" key already covers "I'm done typing". Filters are
  what make an explicit action real — it would apply a pending filter set rather
  than re-run a search that already ran. Deciding it as one piece also avoids
  the in-field icon-slot conflict: leading is a decorative magnifier, trailing
  is the Clear (X) button, and both are occupied exactly when a search button
  would matter.
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
- **Backlog:** real backdrop blur (glassmorphism) for the genre chip on
  `GameCard` (Search/Home results), which currently uses flat semi-transparent
  color (`Color.DarkGray.copy(alpha = 0.6f)`) instead of blurring the cover
  image behind it — a native Compose limitation (no built-in backdrop-filter).
  Evaluate the Haze library (chrisbanes/haze) when this is addressed, and only
  as a single holistic pass — not one new library per individual case. Scope
  note: `TopAppBar` and `BottomNavBar` were evaluated for the same treatment
  and excluded — both currently render on solid/opaque `colorScheme.surface`,
  not transparency, so there's no flat-transparency problem for blur to solve
  there.
- **Backlog:** Open Source Licenses screen (Settings/About) listing every
  third-party library and its license (Apache 2.0 for Haze, OFL for fonts,
  and anything else added later). Real requirement before publishing to the
  Play Store, not before — no Settings/About screen exists yet in MVP scope.

## Related Documents
- `CLAUDE.md` — technical/architectural rules for AI agents working on this repo
- `.claude/skills/` — reusable procedures (discovery-feature, new-feature,
  new-mapper, new-repository-interface, new-repository-impl, new-api-service,
  new-room-dao, new-hilt-module, new-usecase, new-viewmodel, new-screen,
  write-tests). CLAUDE.md's "Available Skills" section is the authoritative
  list — if these two disagree, that is drift.
- `docs/project/retired-skills/` — Skills that have been withdrawn from
  `.claude/skills/`, kept as a record of how the project was built. Not
  invocable. Currently: `project-scaffold`.
- `docs/project/design/` — approved Stitch exports (HTML + PNG) per screen,
  plus `design/states/` for the shared loading/empty/error states
- `docs/project/DESIGN.md` — visual design system (colors, typography, spacing)
- `docs/project/DRIFT-CHECKLIST.md` — manual audit checklist against CLAUDE.md Tier 1 rules