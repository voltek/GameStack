# ADR 0030 — A deferral and a prose defect carry their grounds

Status: Accepted
Date: 2026-10-23
Backfilled: no
Supersedes: ADR-0029 — wording, duplication and friction without a failure scenario are cheap or deferred
Enforced by: none (prose only — .claude/skills/ship-a-branch/SKILL.md § Triage every finding into one bucket, and say which) — the three grounds for Defer, the quoted evidence of a prose defect, the raw output saved before any digest, the table file's shape, the chat's one line per finding
Enforced by: none (prose only — .claude/skills/ship-a-branch/SKILL.md § 4. Write the PR body) — a `Dismissed:` entry names its round, number, claim and verified reason
Enforced by: none (prose only — .github/pull_request_template.md § Verification) — the `Dismissed:` hint carries the same shape

## Context
ADR 0025 made the human confirm every bucket and ADR 0029 split the fix bucket,
but `Defer` was still defined by examples: design, performance, polish, debt.
Any valid finding fitted one of them, so the bucket was chosen by judgement
alone. A governing document's defect needed a failure scenario, which the table
only had to assert. The triage file's shape, and saving the raw output before
digesting it, lived in the agent's memory, which another clone never reads.

## Decision
A valid finding is deferred only on one of three grounds: it is outside the
branch's unit of work, a document already defers it, or its fix needs a plan.
A valid finding on none of them is fixed on the branch; a non-defect fix too
large to be cheap counts as needing a plan. A real defect is still deferred
only by the human.

A `Fix (defect)` in a governing document quotes its evidence in the table:
both contradicting clauses, the broken reference, or the step an agent cannot
carry out. A row that cannot quote it is not a defect.

Each round's raw output is saved verbatim before any digest, and its table
follows the shape `ship-a-branch` gives. The chat carries one line per finding;
the file carries the rest. A dismissal in the PR body names what it dismissed,
because the saved table is not in the repository.

## Consequences
A deferral became checkable against three grounds rather than a judgement, and
a prose defect against the text it quotes. The cost was accepted knowingly: a
valid polish finding inside the branch's scope could no longer leave as debt,
so it was fixed or argued down to a dismissal.
The folder that holds the records stayed in memory, because it is a path on one
machine.
