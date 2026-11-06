# ADR 0052 — The spend warning leaves the repository

Status: Accepted
Date: 2026-11-05
Backfilled: no
Supersedes: ADR-0050 — "The owner is warned before a large batch of runs" and "Ordinary subagent use needs no warning" (Decision)
Enforced by: none (prose only — .claude/rules/governance.md § What earns a line in CLAUDE.md) — no rule on a batch's spend, item 5 as its ground

## Context
ADR 0050 warned before "a large batch" without saying what large meant, and an
arbitrary count of about five filled the gap. The line loaded by path, when a governing
document was touched, so launching a batch did not load it. One batch had prompted
it: 48 `claude -p` runs spent $5.65 in 6.4 minutes and reached the 5-hour limit. It
started with 42% of that window used and would not have reached the limit on a fresh
one.

## Decision
No repository rule sizes a batch of runs or warns about spend.

## Consequences
An agent launching a batch had no line telling it to warn. The owner read the window
in the status line before launching anything costly.

A usage limit belonged to the owner's account, not to this repository, so a clone
lost little.
