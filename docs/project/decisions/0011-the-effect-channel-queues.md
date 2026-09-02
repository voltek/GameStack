# ADR 0011 — The effect `Channel` queues; one user intent emits exactly one effect

Status: Accepted
Date: 2026-08-22
Backfilled: yes — DRIFT-LOG.md, rows 2026-08-22 ("Code review of PR #3 found six defects") and ("A second Codex review pass found two defects")
Enforced by: none (prose only — CLAUDE.md § Architecture)

## Context
A review of PR #3 found six defects the `build`/`test`/`lint` gate had passed
over, one of them a rendezvous `Channel` replaying a duplicated navigation
effect when the user returned to the screen. A second review pass then found the
guard against duplicates, `rememberSingleClick`, had been applied to the result
cards only — the empty-state "Go To Library" CTA could still queue a second
navigation effect. The wording of the rule was list-centric, which is precisely
why the CTA was missed.

## Decision
`UiEffect` is exposed as a `Channel` plus `receiveAsFlow()`, never as
`SharedFlow(replay = 0)`. The `Channel` **queues** anything sent while the
screen is not collecting, and that is deliberate: a snackbar raised during a
configuration change has to survive, and `SharedFlow` would silently drop it.

The cost of queueing is that a *duplicated* effect is not merely handled twice —
it is replayed when the user comes back. So the invariant is placed at the
source: **one user intent must emit exactly one effect.** Wrap every callback
that emits a navigation effect in `rememberSingleClick` — a list's items, an
empty-state CTA, a toolbar action, all of them; share one wrapper across a whole
list so two items cannot fire back to back. Auditing this is **per screen, not
per list**.

Never "fix" a duplicated effect by changing the `Channel`.

## Consequences
One-shot events survive a configuration change instead of vanishing, which is
the failure mode that would be invisible in testing and reported as flakiness.

The trade is explicit and one-directional: the queue turns a dropped effect into
a duplicated one, and duplication is the failure this project would rather have,
because it is visible. Swapping the `Channel` for a `SharedFlow` to silence a
duplicate trades a visible bug for a silent one.

Nothing mechanical checks that every emitting callback is wrapped. It is a
per-screen reading task, and the one time it was done per-list rather than per
screen it missed a control.
