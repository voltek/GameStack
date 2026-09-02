# ADR 0010 — One nested graph per tab; never `NavController.currentBackStack`

Status: Accepted
Date: 2026-08-22
Backfilled: yes — DRIFT-LOG.md, row 2026-08-22 ("Neither CLAUDE.md nor the Spec said how the bottom nav back stack works")
Enforced by: ./gradlew lint

## Context
Neither CLAUDE.md nor the Spec said how the bottom navigation's back stack was
supposed to work, so `Detail` had been implemented as a top-level sibling of the
tabs. It was then swept into the tabs' `saveState`/`restoreState` without
belonging to any tab: leaving a tab from an open Detail and coming back restored
that Detail under no selected tab at all. The workaround written for it reached
for `NavController.currentBackStack` — `@RestrictTo` library-group API, which
made `./gradlew lint` fail with a `RestrictedApi` **Error** and therefore failed
the gate.

## Decision
Each tab is a nested graph — `HomeGraph`, `SearchGraph`, `LibraryGraph` — whose
start destination is that tab's screen. `Detail` is registered *inside every tab
graph* through the shared `NavGraphBuilder.gameDetailDestination()` helper, and
never once at top level. Bottom nav items and
`navigateToBottomNavDestination()` always target a `*Graph` route, never the
screen inside it. `NavController.currentBackStack` is never called.

The graphs are structure, not tabs: there are still exactly three.

## Consequences
Each tab owns its own back stack, and the intended behaviours follow from that
rather than from special cases: a Detail belongs to the tab that opened it and
Back returns to that tab's screen; switching tabs saves the whole originating
stack, Detail included, and restores it on return; Back from a non-start tab
goes to Home and then exits, which is standard Android behaviour and not a
defect to fix.

If tab switching ever appears to need `currentBackStack`, the graph structure is
wrong — the restricted API is a symptom, not a missing tool.

What `lint` actually enforces is the second half: the restricted-API call fails
the gate mechanically. The structure itself — Detail inside every graph, nav
items targeting `*Graph` routes — is guarded only by a checklist detector and by
reading. The stronger rung covers the smaller clause.
