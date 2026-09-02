# ADR 0002 — The Repository owns the `updatedAt`/`completedAt` write rules

Status: Accepted
Date: 2026-08-04
Backfilled: yes — DRIFT-LOG.md, row 2026-08-04 ("A decision with no incident behind it: the `completedAt` transition rule")
Enforced by: none (prose only — CLAUDE.md § Data Sources)

## Context
`completedAt` must be stamped only when a game's `listStatus` *transitions* into
COMPLETED, and never touched by a rating change; `updatedAt` is stamped on every
write. Detecting that transition requires comparing the incoming value against
the row already stored, and every layer that could hold the comparison broke a
rule to do it. A UseCase cannot see stored persisted state without reaching past
the Repository; the DAO is a generated interface with nowhere to put it; and the
Repository is explicitly forbidden from containing business logic.

## Decision
Both timestamps are **persistence invariants**, not domain rules, and the
Repository owns them as a documented exception to "Repositories contain no
business logic". Every write is read-modify-write: load the existing row,
produce the updated copy, upsert. `updatedAt` is stamped on every write;
`completedAt` is stamped only when the incoming `listStatus` is COMPLETED and
the stored one was not.

The boundary that still holds: the Repository decides *when* a timestamp column
is written, never *what the timestamps mean to the user*. Formatting and
ordering stay in Domain and UI. A rule needing more than a comparison of old and
new persisted state is real business logic and belongs in a UseCase.

## Consequences
The exception is narrow and stated, so the general rule survives it — the
checklist's Repository detector names this carve-out explicitly and treats
everything else as drift.

The cost: "no business logic in Repositories" is now a rule with an exception,
and the next borderline case has to be argued against the boundary above rather
than against an absolute.

This decision is unenforced by anything mechanical, and will stay that way
longer than most: `UserGameEntity` does not exist yet, so there is no code for a
test to hold. The transition test belongs in the same change that first writes
the entity, and that is the point at which this line should stop reading `none`.
