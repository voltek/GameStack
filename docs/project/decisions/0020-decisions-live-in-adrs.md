# ADR 0020 — Decisions live in ADRs, incidents live in the drift log

Status: Accepted
Date: 2026-08-31
Backfilled: no
Enforced by: none (prose only — CLAUDE.md § Self-Healing Loop)

## Context
`DRIFT-CHECKLIST.md` had grown to 193 lines and roughly 58KB while holding three
artefacts with different lifecycles: 45 violation detectors, which are bounded
and barely grow; a Resolution log of 56 rows, which is unbounded and grows with
every incident; and architectural decisions buried inside those rows. Keeping
the bounded and the unbounded in one file is precisely why it reached that size.
The buried decisions were the sharper problem: the force-push window changed
criterion, and understanding it meant reading two rows six apart and noticing
they contradicted each other. Nothing in the repository stated what any rule's
enforcement actually was, so a rule in prose and a rule with a test behind it
looked identical — and this project has rules that were violated two and three
times over despite being written down.

## Decision
Three files, three jobs, three growth laws.

- `DRIFT-CHECKLIST.md` holds only the violation detectors. Imperative; grows
  only when a check is added.
- `DRIFT-LOG.md` holds the incidents — every time code and documentation
  disagreed and which of the two was changed. Past tense; unbounded; ordered
  newest first.
- `decisions/NNNN-slug.md` holds one decision each, in force, present tense,
  numbered by the date of the original decision and never renamed.

Every ADR carries a mandatory `Enforced by:` from a closed vocabulary. A
decision that is born or changes is an ADR, never a log row; an incident is a
log row, never an ADR. Where one drift produces both, the row points at the ADR
instead of restating it. The format, the vocabulary and the immutability rule
live in `decisions/README.md`.

## Consequences
Each file now has one reason to grow, so the checklist stays readable as
automation targets and the log stays appendable without dragging rules along.
A decision that changes is legible as two files — the superseded one and its
successor — rather than as two contradicting rows.

`Enforced by:` turns the ADR set into the map of what is genuinely policed
versus merely written down. That map is expected to read mostly `none`, and that
is the finding, not a defect to dress up.

The costs, accepted knowingly: a drift that both records an incident and changes
a decision now touches two files instead of one, and the discipline of pointing
rather than restating is what keeps them from drifting apart. There is
deliberately no ADR index, so the set is only enumerable by `grep` — the
alternative was a second place to update per ADR, which this repository has
already been burned by.

Backfilling the decisions that predate this file is deliberately a separate
change. Until it lands, this folder holds only ADR 0020 and the numbers
0001–0019 are unassigned. That gap is expected and must not be "fixed" by
renumbering: the numbering follows the date of the original decision, and those
decisions are older than this one.
