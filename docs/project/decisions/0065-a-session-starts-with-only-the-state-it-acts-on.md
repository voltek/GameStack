# ADR 0065 — A session starts with only the state it acts on

Status: Accepted
Status: Superseded by ADR-0067 — "plans while an open item cites them, deleted once every decision they hold is in an ADR, an issue or TOOLING.md" and "review records until their branch merges" (Decision)
Date: 2026-11-11
Backfilled: no
Supersedes: ADR-0047 — "first after any bound warning" (Decision)
Enforced by: test: test-session-start.sh#the triage line before the warnings — the triage line printed first
Enforced by: test: test-session-start.sh#a section outside the allowed ones — a handoff section other than *Now*, *In flight* and *Owed by the owner* warned about
Enforced by: test: test-session-start.sh#sections match the checkpoint Skill — the hook's allowed sections named in the Skill
Enforced by: test: test-session-start.sh#over the line bound — the handoff's bound of 40 lines
Enforced by: test: test-session-start.sh#over the byte bound — the handoff's bound of 3,000 bytes
Enforced by: test: test-session-start.sh#a plan nothing cites — a plan neither file cites warned about
Enforced by: test: test-session-start.sh#the plan list capped — ten plans named at most, the rest counted
Enforced by: test: test-session-start.sh#what blocks the product listed — of the open issues, only what blocks the product listed by name
Enforced by: none (prose only — .claude/skills/checkpoint/SKILL.md § HANDOFF.md — session state only) — *In flight* holding only the state of open work
Enforced by: none (prose only — .claude/skills/checkpoint/SKILL.md § CYCLE.md — one finite cycle) — a status as a state, the closed cycle's file name
Enforced by: none (prose only — .claude/skills/checkpoint/SKILL.md § What is kept) — what is kept beside the two files, and a plan deleted only after its decisions are checked
Enforced by: none (prose only — docs/project/TOOLING.md § 14. Session handoff) — a line of the hook's output admitted only when a session acts on it at its start

## Context
ADR 0045 moved the rules of the session handoff into the `checkpoint` Skill, and
the Skill already said that what a session learned never went into the handoff.
Its fourth section, *Constraints not written anywhere else*, was the way around
that rule: it and *In flight* filled with lessons, methods and decisions that no
clone read, gathered in #88. The cycle's statuses did the same, holding lists of
pull requests and an owner's decision, so the cycle file was the largest part of
the hook's output, about 5,700 characters in all. That output also repeated the
git status and the last commits, which Claude Code injected at a session's start
anyway, and named every open defect, a list no session acted on. What was kept
beside the two files had no written rule: #87 had decided it, and the Skill
marked it pending.

## Decision
A session starts with the state it acts on and nothing else.

- **The handoff holds three sections:** *Now*, *In flight* and *Owed by the
  owner*. *In flight* is the state of open work, each line a pointer and where it
  stands; a rule, a lesson, a method or a decision goes to its home. The hook
  warns about any other section, and the handoff's bound is 40 lines and 3,000
  bytes.
- **A cycle's status is a state:** `todo`, `in progress`, `done` with its date and
  pull requests, or `back to the pool`.
- **A line of the hook's output is admitted only when a session acts on it at its
  start and Claude Code does not inject it already.** What only a triage reads, the
  triage computes. The hook prints the local branches, the next ADR number, the
  issue pool's counts and the issues that block the product; not the status, the
  log or the defects. Whether a triage is owed comes first, before any warning:
  the warnings grow with the plans, and a cut preview still shows it.
- **Kept beside the two files:** plans while an open item cites them, deleted once
  every decision they hold is in an ADR, an issue or TOOLING.md, with the hook
  naming the plans neither file cites, the first ten and a count of the rest; review records until their branch merges,
  with the tally frozen; and closed cycles as `YYYY-MM-cycle-N.md`, the month each
  closed, until #159 gives the cycle a shared home. Former handoffs are not kept.
- **Running on Windows** is written in TOOLING.md, outside its entries, since
  nothing in the repository configures it.

## Consequences
The hook's output fell by about a third, and a handoff that grew a fourth section
or passed a third of its old bound was flagged at the next session. The
constraints the section held moved to TOOLING.md § 9, § 11 and the section on
running on Windows. Deleting a plan stayed a manual step, because it could not be
undone and needed its decisions checked first; the hook only named the
candidates, and until they were checked it named them at every start. A defect
no longer showed at a session's start: one that mattered waited for the triage or
for the owner to name it. Keeping the closed cycles kept the only record of what
each cycle chose, at the cost of three small files on one machine.
