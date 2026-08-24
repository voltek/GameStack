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

## Why this project exists, and what it optimises for
GameStack is a real product headed for the Play Store, with everything that
implies. It is also, deliberately, a **case study in building software well with
AI assistance** — that is the reason it is being built at all.

Both are true at once, and the second one changes engineering decisions that
would otherwise be settled by convention. Recorded here because those decisions
keep needing a justification, and without this section each one looks arbitrary:

- **The repository's history is part of the deliverable.** `git log` is study
  material, not only a changelog. This is why `Create a merge commit` is the
  default merge strategy, why a branch showing "patch, patch, patch, redesign"
  was kept intact rather than squashed, and why commit messages carry diagnosis
  rather than a summary of the diff.
- **Process work is product work.** The Skills, the tiered rules, the
  Resolution log and the review loops are not overhead around the app — they are
  a second thing being built, and often the more valuable one.
- **A decision is worth more written down than made quickly.** Where a
  conventional default exists, it is still stated explicitly, with the trade.

None of this licenses gold-plating the app itself. Product scope is exactly what
the MVP section below says it is.

## Core Features (MVP)

### Search
- Search games by name via the IGDB API.
- Each result shows the game's cover, name, primary genre, and developer
  (IGDB's `genres` and `involved_companies` fields) — confirmed as part of
  building the Search feature, matching the approved `docs/project/design/search/`
  mockup. Distinct from search filters (letting the user filter results by
  genre/platform), which remains backlog — see Explicitly Deferred.
- The screen has four states, not three. Before any query is typed it shows an
  **initial state** ("Search for games" + guidance), not a blank canvas; then
  loading, results, or "No Results Found". The initial state was missing from
  the original mockups — surfaced by code review of the Search PR, which found
  the screen rendering an empty grid on first open — and is now specified in
  `docs/project/design/search/search-initial.html`.
- A **failed pull-to-refresh keeps the results already on screen** and reports the
  failure as a banner above them, with Retry, rather than replacing them with the
  error state — those results still answer the query, and losing them to a
  transient network blip is worse than a stale list. The banner stays as long as
  the situation does and disappears on its own when a refresh succeeds or the
  query changes; it is not a transient toast, because "these results are stale"
  is a condition, not a moment. A failed search for *new* text is not the same
  case and does show the error state: the results still displayed answer the
  previous query, so keeping them would misrepresent what was searched.

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
- (Optional / deferred) AI-based recommendations based on
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

## Explicitly Deferred
- **AI-powered game recommendations** based on the user's history (Room data).
- **Instrumented tests (`androidTest/`).** Everything currently tested runs on
  the JVM. Real-SQLite DAO tests, Room migration tests, and true device
  interaction need a device/emulator and are deferred. Three concrete triggers,
  so this is not "some day" — in priority order:
  1. **The first Room migration.** A wrong `@Query` or a missing migration passes
     every existing test and fails on device, and it fails by *destroying real
     users' data*. This is the only item on the list whose cost is irreversible,
     and it is the trigger to act on. No Room table exists yet.
  2. **Real gestures and IME** — pull-to-refresh, the soft keyboard. Robolectric
     screen tests cover render and semantics but not these.
  3. **A pre-release smoke test.** Partly free already: Play Console's
     pre-launch report runs the app on real devices when a build is uploaded to a
     test track, so this trigger is weaker than it looks.

  Compose *screen* tests are **no longer part of this deferral**: they run under
  Robolectric on the JVM (CLAUDE.md → Testing Stack). They share the one Gradle
  task for now; splitting them out is queued as tooling work, triggered by the
  second screen-test class (`docs/project/TOOLING-BACKLOG.md`).
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
- **Backlog (low priority for now, but with a deadline):** accessibility. MVP
  does not target it — no TalkBack pass, no large-font or contrast audit, no
  touch-target review. Accessibility findings from code review are non-blocking
  while this holds. Two limits on the deferral, both in CLAUDE.md → Accessibility:
  a change may not *remove* an affordance that already worked (replacing a
  Material component with a hand-built one is where this happens silently), and
  the deferral expires before the first Play Store release — Google publishes
  accessibility expectations, and retrofitting semantics across a finished UI is
  a rewrite rather than an addition.
- **Backlog (tooling):** consider adopting OpenSpec as an active change-tracking
  layer alongside CLAUDE.md, IF the project grows beyond solo development or
  manual tracking via Skills becomes a real pain point. Not worth the added
  infrastructure at current scale.
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
  plus `design/states/` for the shared loading/empty/error states.
  `design/README.md` covers how to regenerate a PNG from its HTML and what a
  screenshot costs to read
- `docs/project/DESIGN.md` — visual design system (colors, typography, spacing)
- `docs/project/DRIFT-CHECKLIST.md` — manual audit checklist against CLAUDE.md Tier 1 rules
- `docs/project/TOOLING-BACKLOG.md` — repository tooling planned but not built
  (review automation, hooks). Harness, not product, which is why it is not here