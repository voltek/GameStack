# ADR 0007 — The force-push window closes at the first review

Status: Superseded by ADR-0014
Date: 2026-08-22
Backfilled: yes — DRIFT-LOG.md, row 2026-08-22 ("Tier 1's blanket")
Enforced by: none (prose only — CLAUDE.md § Git workflow)

## Context
Tier 1 carried a blanket "never force-push a branch that has already been
pushed". That made `Squash and merge` the only available remedy for an untidy
branch, which contradicted the merge-strategy rule adopted the same day —
merge commits are the default precisely to keep `blame` and `bisect`
granularity, and squashing throws that away. The absolute rule was guarding
against overwriting a collaborator's work, a risk that does not exist in a repo
with one contributor.

## Decision
Tier 1 keeps the absolute bans: never force-push `main`, never a branch another
person may hold, and never bare `--force` anywhere.

Tier 2 permits `--force-with-lease` on your own feature branch **until its first
review**. The window is anchored to the first review rather than to the first
push because rewriting commits orphans review threads, and that thread history
is what caught two defects the gate passed on PR #3.

## Consequences
An untidy branch can be cleaned before review without spending the granular
history a squash would destroy, so the merge-strategy rule stops being
self-defeating.

The rule is deliberately conditional where an absolute one would be easier to
obey. That trade is accepted knowingly: an absolute ban is cheaper to remember
and was, here, wrong often enough to be worth the extra clause.

The cost: the window is narrow, and a branch that draws a review early is frozen
early — its history has to be right the first time or stay untidy.
