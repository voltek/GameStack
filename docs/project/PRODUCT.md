# GameStack — Product

What the app is, for whom, what it does, what has to be true before it ships,
and what it will not do. A living document: it changes only on the triggers in
*How this document changes*, through a branch, and git is its version history.
Technical rules live in `CLAUDE.md`; each capability's behaviour lives in its
spec under `openspec/specs/` (ADR 0041). Until ADR 0043 it was the Spec,
`GameStack-Spec-v1.md`, the name frozen ADRs and history still use.

## What it is
GameStack is a native Android app for exploring video games and keeping a
personal game library, using the IGDB API as the data source. Users search for
games, read their details, keep them in personal lists and rate them.

**The catalog is the way in; the library is the reason to come back.** When the
two pull a decision different ways, the library wins. What sets it apart from
other IGDB-based apps is that the library needs **no account and no server of
ours**: it leaves the phone only in the user's own Android backup.

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

- **Learning outranks product completeness.** The app should be useful enough
  that publishing it means something, but what it is for is the whole path, from
  the architecture and the harness to a Play Store release. Where serving users
  more fully and learning that path disagree, the path wins (ADR 0044).
- **The repository's history is part of the deliverable.** `git log` is study
  material, not only a changelog. This is why `Create a merge commit` is the
  default merge strategy, why a branch showing "patch, patch, patch, redesign"
  was kept intact rather than squashed, and why commit messages carry diagnosis
  rather than a summary of the diff.
- **Process work is product work.** The Skills, the tiered rules, the ADRs and
  the review loops are not overhead around the app — they are a second thing
  being built, and often the more valuable one.
- **The harness is meant to leave.** What is built around the app — rules,
  Skills, hooks, the review loop, the handoff (PENDING — not yet defined) —
  is meant to be taken to other projects, new ones and ones already in
  production. So a lesson counts once it is recorded where another project can
  read it, as CLAUDE.md § Purpose says, never only outside the repository. And
  product keeps moving because it is the part of the harness's test that
  documentation branches cannot run.
- **A decision is worth more written down than made quickly.** Where a
  conventional default exists, it is still stated explicitly, with the trade.

None of this licenses gold-plating the app itself. Product scope is exactly what
*Capabilities* says it is.

## Capabilities
One row per capability. *Spec* names its folder under `openspec/specs/` once a
change has written one; until then the capability's section below is all there
is, and the change that writes its spec removes that section.

| Capability | Spec |
|---|---|
| Search | `openspec/specs/search/` |
| Game Detail | none yet |
| Personal Lists | none yet |
| Personal Rating | none yet |
| Home | none yet |
| Navigation | none yet |

### Game Detail
- Name, description
- IGDB community rating
- Official trailer
- External purchase links (when available from IGDB)

### Personal Lists
Three lists, shown in Library: Playing Now, Want to Play, Completed. A game is
in at most one of them; abandoning a game means removing it from its list.

### Personal Rating
Thumbs up or thumbs down, or unrated. A rating means "I like it", not "I
finished it and liked it", so it is independent of the lists: a game can be
rated in any list or in none. Never synced with IGDB's community rating.

### Home
- Most popular games today
- Games the user has recently interacted with

### Navigation
Persistent bottom navigation bar with 3 top-level destinations: Home, Search and
Library. Game Detail is not a top-level destination — it is reached by tapping a
game card from any of the three.

Each tab remembers where you were. Opening a Game Detail from a tab keeps that
tab selected, and leaving the tab and coming back returns you to exactly where
you left off — including to an open Game Detail, not the tab's starting screen.
Back from a Game Detail returns to the list it was opened from; Back from a
non-Home tab returns to Home.

### Constraints on every capability
- **Data:** game data comes from IGDB, read-only. Lists and ratings are stored on
  the phone, never sent to a server of ours or to IGDB; the only copy that
  leaves it is the user's own Android backup.
- **Look:** dark theme only, with the fixed palette in `DESIGN.md`.
- **Orientation:** portrait-only, locked in the manifest (ADR 0019): every
  screen was designed portrait, and landscape is a second layout decision per
  screen. The `verticalScroll` on `MessageState` stays: it keeps the CTA
  reachable with the keyboard raised in portrait. Tablets and foldables wait
  with landscape, revisited together (#120).

## First release
The first Play Store release ships when all of these hold. Success is meeting
them; no usage metrics are collected. Each points at its `product` issue
(*Backlog* below).

- The four screens (Home, Search, Library, Detail) built, and every capability
  they show with its spec — #106, #107, #108.
- Accessibility pass: TalkBack, large font, contrast, touch targets (CLAUDE.md
  § Accessibility) — #109.
- Open Source Licenses screen listing every third-party library and its
  license — #110.
- A basic security audit: no secret inside the APK, R8 on and `debuggable` off,
  HTTPS only, no token in release logs, the fewest exported components, and what
  a backup holds. The basics, not hardening — #111, #112.
- IGDB/Twitch terms checked for a store release, with the attribution they ask
  for; a privacy policy and the Play Console Data safety form — #113.
- The Room schema exported and frozen: from this release on, a schema change
  needs a migration — #114.
- A release build: R8, signing, an app bundle, the `targetSdk` Play
  requires — #115.
- A device smoke test of the release build — #116.
- Lists and ratings survive a phone change: Android Auto Backup, with a restore
  verified on a device — #117.
- The interface in English and Spanish — #118.
- The store listing: icon, screenshots, description — #119.

## Backlog
Every product item is a GitHub issue labelled `product`, whose Horizon field in
the public `GameStack Product` Project
(https://github.com/users/voltek/projects/1) is **Now** (in the current cycle), **Next**
(needed for the first release) or **Later** (after it). This section holds one
line per Later item, pointing at its issue; the issue holds the detail. Next
items are the release criteria above.

- Landscape, tablets and foldables, revisited together (ADR 0019) — #120
- A "Dropped" list for games left unfinished — #121
- A third rating state, "Loved" — #122
- Recent search history on Search — #123
- Search filters (genre, platform), deciding an explicit search button with them — #124
- Quick-add to a list from a game card, without opening Detail — #125
- Search within the user's own library — #126
- A local user profile (avatar, display name) — #127
- A light theme — #128
- Real backdrop blur behind the genre chip on game cards — #129
- Offline mode: caching IGDB responses, a Repository redesign of its own — #130
- AI on the device, recommendations first; enters only if on-device AI proves
  feasible, never as recommendations without it — #131

**Not in this app**, and no issue: an account or login, cloud AI paid per use,
and anything built on retrieval over the user's data. Each may make more sense
in another app.

## Open questions
Only what is still open. An answered question leaves this list for the place
its answer belongs.

- **How the Twitch client secret leaves the APK.** `local.properties` keeps it
  out of git, but the build compiles it into `BuildConfig`, readable from any
  published APK, and every user would share one rate limit. Likely a small proxy
  that holds the secret; deciding it changes CLAUDE.md § Data Sources (Tier 1).
  Blocks the first release — #111.
- **Whether to measure use after the first release**, once real users exist.

## How this document changes
It is read at fixed moments, never on a schedule: a change reads it for scope,
and a triage reads *First release*, *Backlog* and *Open questions*. It is
edited only when a change adds or retires a capability, an item moves in or out
of scope, an open question is answered, or a release criterion changes. The
*Capabilities* table's *Spec* column names exactly the folders under
`openspec/specs/` (ADR 0043, checked by DRIFT-CHECKLIST.md). A technical
deferral that no release criterion needs is a harness issue, never a line here.

## Related documents
- `CLAUDE.md` — technical and architectural rules; it wins over any spec or change
- `openspec/` — a spec per capability and the changes that alter them, one per
  feature branch (ADR 0041)
- `docs/project/DESIGN.md` — the visual design system (colours, typography, spacing)
- `docs/project/design/` — Stitch exports (HTML + PNG) per screen; where one
  disagrees with a document, the document wins (ADR 0042)
- `docs/project/decisions/` — one ADR per decision in force, each naming what
  enforces it
- `docs/project/TOOLING.md` — the harness: what each tool is and why
- GitHub issues — all pending work: `product` for this document's backlog, the
  rest for the harness (ADR 0038)
