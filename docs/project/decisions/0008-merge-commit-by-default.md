# ADR 0008 — Merge commit by default; rebase-and-merge is banned

Status: Accepted
Date: 2026-08-22
Backfilled: yes — DRIFT-LOG.md, row 2026-08-22 ("The Git workflow said nothing about merge strategy")
Enforced by: none (prose only — CLAUDE.md § Git workflow Tier 2)

## Context
The Git workflow said nothing about merge strategy, nothing about PR size, and
nothing about how a bot's review threads get closed. "One branch per unit of
work" existed but had no point of enforcement. The result was measurable: PR #3
reached 1,992 additions across 46 files with only two of its commits belonging
to the Search feature, and a Codex review thread stayed open on a finding that
had already been fixed.

## Decision
`Create a merge commit` is the default. `Rebase and merge` is **never** used:
replaying commits onto a new base produces intermediate states that never
existed and never passed the gate. `Squash and merge` is a fallback, not the
default. A pushed branch is updated with `git merge main`, never `git rebase`,
for the same reason.

Alongside it: a per-commit checkpoint — does this commit exist *because of* this
branch's unit of work — and 400 changed lines or 15 files as a smoke alarm, not
a limit. Every review thread is answered and then resolved.

## Consequences
`blame` and `bisect` keep pointing at states that actually existed and actually
passed the gate, which is what this repository optimises for.

The cost: history is bushier than a linear log, and a branch that should have
been split shows up as a large merge rather than being prevented outright — the
smoke alarm is a prompt to look, not a gate.

The enforcement picture is worth stating plainly, because it is weaker than it
looks. `main`'s branch protection requires conversation resolution and the
`build / test / lint` status check — those enforce neighbouring rules, not this
one. The repository still has `Rebase and merge` enabled, so the ban on it rests
entirely on prose while the button that would violate it sits one click away.
Closing that is a repository setting, not a document change.
