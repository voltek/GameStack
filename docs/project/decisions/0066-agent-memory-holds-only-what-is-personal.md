# ADR 0066 — Agent memory holds only what is personal or about one machine

Status: Accepted
Date: 2026-11-11
Backfilled: no
Enforced by: test: test-memory-gate.sh#the first write to a memory — the first write to a memory file in a session denied with the question
Enforced by: test: test-memory-gate.sh#the same write sent again — the same call sent again let through
Enforced by: test: test-memory-gate.sh#the index — the index of memories never asked about
Enforced by: commits.yml — test-memory-gate.sh run on every pull request
Enforced by: none (prose only — .claude/skills/checkpoint/SKILL.md § Steps) — what a session learned routed to its home, memory only for what is personal or about one machine

## Context
ADR 0039 put a harness decision where a clone reads it, never only outside the
repository. Agent memory on one machine had become the place where lessons
gathered anyway: #88 counted nineteen memories, of which eight held a rule or a
method another agent or person needed and seven restated what an ADR, a Skill
or the language setting already said. The `checkpoint` Skill's step that routes
what a session learns sent "a preference" to memory, a word wide enough for any
lesson. Nothing asked the question when a memory was written, and a walk through
them found the drift weeks later. A hook can add context to a tool call without
blocking it, but that context reaches the model beside the tool's result, after
the file is written.

## Decision
Agent memory holds what is personal to the owner or about one machine, and
nothing else.

- **What serves a clone or another person goes to the repository:** CLAUDE.md, a
  `.claude/rules/` file, a Skill, TOOLING.md, an ADR or an issue. What serves
  nobody is not written.
- **The question is asked before the write.** A `PreToolUse` hook on Write and
  Edit denies the first write to each memory file in a session, its reason the
  question; the same call sent again goes through. The index of memories is not
  asked about, since it only points at them.

## Consequences
A memory cost one more tool call the first time a session wrote it, and the
question came while nothing was yet written, so a lesson needed no deleting
afterwards. The hook ran on every Write and Edit, a node start each, and
recognised a memory only by its path under `.claude/projects/<project>/memory/`:
a memory folder moved by a setting was not asked about, and neither was a memory
written through the shell, whose commands name a path in more spellings than a
pattern reads. The walk that prompted
this moved three lessons into `checkpoint`, `review-round` and the issue form,
deleted the memories the repository already covered, and kept what was personal
or about the one machine.
