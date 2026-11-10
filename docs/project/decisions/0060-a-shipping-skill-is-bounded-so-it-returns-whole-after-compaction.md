# ADR 0060 — A shipping Skill is bounded so it returns whole after compaction

Status: Accepted
Date: 2026-11-10
Backfilled: no
Enforced by: test: ShippingSkillBoundTest#should keep each shipping Skill within its byte bound

## Context
After a compaction, Claude Code re-attached only the first 5,000 tokens of each
Skill invoked earlier in the session. `ship-a-branch` had grown from 448 to 525
lines, about 31 KB, with nobody reviewing its size, and review round 2 was
skipped twice in sessions that had loaded it (#99). Splitting it into
`clean-branch`, `review-round`, `open-pr` and `merge-pr` made each short, but
nothing stopped one from growing back. CLAUDE.md's own bound was already a test
(ADR 0055).

## Decision
The `SKILL.md` of each of the four shipping Skills holds at most 15,000 bytes, so
it returns whole after a compaction. A rare case goes to the Skill's
`references/`, and its `SKILL.md` says when to read that file. The bound counts
bytes, which over-count characters outside ASCII and so only err safe.

## Consequences
15,000 bytes sat under 5,000 tokens of English prose, an estimate nobody
measured. The bound covered these four Skills, which ran in long sessions and
whose skipped steps had cost a review round; the other Skills stayed unbounded.
A file read from `references/` did not return after a compaction either, which
is why its `SKILL.md` said when to read it.
