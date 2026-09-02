# ADR 0019 — The app is portrait-only; landscape is deferred

Status: Accepted
Date: 2026-08-23
Backfilled: yes — DRIFT-LOG.md, row 2026-08-23 ("Nothing said whether the app supports landscape")
Enforced by: none (prose only — GameStack-Spec-v1.md § Explicitly Deferred)

## Context
Nothing said whether the app supports landscape. Every screen had been designed
portrait and none had ever been checked rotated, so the app was shipping an
orientation it had neither been designed nor tested for. A `verticalScroll` had
already been added to `MessageState` *for* landscape, which made the gap look
addressed when it was not — a partial accommodation reads as a decision.

## Decision
The MVP is portrait-only, locked in the manifest with
`android:screenOrientation="portrait"` on `MainActivity`. Landscape is deferred
and recorded as such in the Spec, with the reason: supporting it is a second
layout decision per screen, not a free consequence of Compose.

Two carve-outs, so they are not undone by accident:

- The `MessageState` `verticalScroll` **stays**. What it actually protects is
  the CTA with the keyboard raised in *portrait*, which is the case that ships.
- Tablets and foldables are the same deferral, to be revisited together, since a
  portrait lock reads very differently on a large screen.

## Consequences
The app now ships only the orientation it was designed and verified in, and
"rotate the device" stops being an untested path that anyone could reach.

The cost: a user who rotates gets nothing, and the deferral grows more expensive
the more screens exist when it is finally taken up — which is why the tablet and
foldable question is pinned to the same revisit rather than left to surface on
its own.

This rule lives in the Spec rather than in CLAUDE.md, which is why its
`Enforced by` names that document: it is product scope, not an architectural
constraint on code that could be written on any turn. Verified at the time by
forcing `user_rotation=1` on the emulator — the app stayed portrait.
