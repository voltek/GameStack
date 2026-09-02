# ADR 0016 — Robolectric is permitted for Compose screen tests and nothing else

Status: Accepted
Date: 2026-08-23
Backfilled: yes — DRIFT-LOG.md, rows 2026-08-23 ("Testing Stack (Tier 1) forbade Robolectric") and ("The measurement justifying the new Robolectric rule was wrong")
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Testing Stack

## Context
Testing Stack Tier 1 forbade Robolectric outright so that `./gradlew test` would
stay fast. The rule predated the question it was blocking: Compose screen tests
under Robolectric run on the JVM in seconds, and they are the only thing that
can reach Composable-level logic. Two real gaps were sitting behind the ban —
the `rememberSingleClick` fix on the empty-state CTA had no test at all, and
nothing asserted that the refresh banner declares a live region.

The measurement first used to justify the narrowed rule was itself wrong, in
three documents at once, and had to be re-derived: the cost **scales at roughly
a second per screen test rather than amortising** as a per-class startup, and
Robolectric is most of `testDebugUnitTest`.

## Decision
Robolectric is permitted **for Compose screen tests, and nothing else** — not
for DAOs against real SQLite, and never as a substitute for a test that already
runs pure. A screen test asserts what renders in each declared state and what
semantics a node carries; it cannot assert that TalkBack speaks an announcement,
nor anything gesture- or IME-dependent. Those need a device.

The suites stay one Gradle task for now, and the trigger to split them is an
**event, not a number**: the second screen-test class, which is the concrete
thing that roughly doubles this. No exact figure is recorded, on purpose —
measure when you need it, with `--rerun-tasks` and the XML results.

## Consequences
The two untested gaps became testable, and the first screen test written under
the new rule immediately found that `rememberSingleClick` swallowed the *first*
click rather than the second — a defect no manual pass could ever have caught,
since on a real device uptime is hours by the time a screen is interactive.

The cost is paid on every run: these tests are roughly a second each against
milliseconds for everything else, and that cost grows with each one rather than
being absorbed. Splitting the Gradle task is TOOLING-BACKLOG item 4.

An exact figure was recorded three times and went stale within the same branch
each time. The event-shaped trigger is the correction: an event does not decay.
