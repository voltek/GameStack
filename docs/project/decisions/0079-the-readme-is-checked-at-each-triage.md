# ADR 0079 — The README is checked at each triage, and its measured section is never refreshed

Status: Accepted
Date: 2026-11-12
Backfilled: no
Enforced by: none (prose only — .claude/skills/checkpoint/SKILL.md § Triage) — the check at each triage, and the measured section left as measured

## Context
The README predated the harness and had promised to "grow as features land", a
promise of upkeep nothing kept: a status list, counts and a description that went
stale as the repository moved (#100). Its rewrite added numbers measured once, on
2026-11-12: a fresh session's tokens before any task, and the review loop's rounds.
A per-session measure was dropped (#194): at this scale it cannot separate the
model's effect from the task's. A `gated` issue to check the README was ruled out,
since it waits for one trigger and closes, and a recurring one stays open in the
pool.

## Decision
At each triage, check whether the README still describes the repository; where it
does not, propose an issue. Outside its measured section the README states no count
of what the repository holds, such as how many Skills or ADRs it has; that section
names its date and is never refreshed.

## Consequences
The README was read where the whole pool already was, once per cycle. Its lines
that a merge makes stale, such as a feature's status or what is not built yet,
stayed stale until the next triage. The measured section aged visibly by its date rather than silently, and
what it measured held only for the commits, the Claude Code version and the model
it names. The data behind it, a tally and a baseline kept outside the repository,
was deleted once the README quoted it: the README's section became its record.
