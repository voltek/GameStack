# ADR 0039 — The project's purpose is a rule loaded on every turn

Status: Accepted
Date: 2026-10-28
Backfilled: no
Enforced by: none (prose only — CLAUDE.md § Purpose — Tier 1) — weighing against both halves, where a harness decision lives
Enforced by: none (prose only — docs/project/PRODUCT.md § Why this project exists, and what it optimises for) — the full statement, the harness meant to leave

## Context
The Spec's *Why this project exists* said GameStack was a product and a case
study, and that process work was product work. It had been placed in the Spec
rather than CLAUDE.md on purpose, because it was read on demand (DRIFT-LOG.md,
row 2026-08-23 ("The merge-strategy, keep-the-messy-history")). It did not say
that the harness was meant for other projects, new ones and ones already in
production, and being read on demand, it was never in front of an agent
proposing work. The owner's fuller statement of the purpose lived only in an
out-of-repository plan and in one machine's memory. Lessons meant for other
projects had been left in that memory, which a clone did not carry, and two
months of governance branches had run without moving the product.

## Decision
CLAUDE.md carries the purpose as a Tier 1 rule, short enough to be paid for on
every turn: half product, half a harness meant for other projects, and both a
case study. Weigh every proposal and decision against both halves and say what
it gives each. A harness decision lives where a clone reads it — an ADR,
TOOLING.md, a Skill — never only outside the repository, in memory or an
out-of-repository file. Governance that serves neither the next product step nor
what another project would take is a cost.

The Spec's *Why this project exists* holds the full statement, including that
the harness is meant to leave; the rule points at it and does not restate it.

## Consequences
The purpose reached every request that loaded CLAUDE.md, so a reviewer loading
it could judge a change against it as well as the agent that wrote it. Saying
what a proposal gave each half made the weighing visible to the owner rather
than implied. CLAUDE.md was over ADR 0031's bound, so the addition was offset by
compacting *What is this project* and the retired Skills entry, whose reason
the archived Skill already held.

Accepted knowingly: every proposal carried a statement for each half, which
could harden into a ritual line, and only judgment policed the rule. The rule
was also broken the day it was written: the handoff system, the cycle's
admission and insertion rules and the archive conventions for review records
and done plans lived only in out-of-repository files, tracked in #87; the
verification round's limit of three rounds lived only in memory, tracked in
#85; and harness lessons lived only in one machine's agent memory, tracked in
#88.
