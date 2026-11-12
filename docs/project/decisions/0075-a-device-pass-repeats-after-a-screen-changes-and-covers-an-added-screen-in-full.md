# ADR 0075 — A device pass repeats after a screen changes, and covers an added screen in full

Status: Accepted
Date: 2026-11-12
Backfilled: no
Enforced by: none (prose only — .claude/skills/verify-on-device/SKILL.md § What to cover) — the pass repeated for the states a later commit changed, and an added screen's exploratory pass
Enforced by: none (prose only — .claude/skills/verify-on-device/SKILL.md § A defect found) — a defect the branch caused fixed in it, any other opened as an issue

## Context
ADR 0005 had every change that adds or alters a screen verified on the emulator before
a merge was requested, but said nothing of the commits that came after the pass: a
review's fix or a merge of `main` could alter a verified screen, and the PR still
claimed the earlier pass. The exploratory pass, the checks that had found defects
before (process death, a configuration change, offline and retry, input at the
edges), ran only over a merged screen, so a new screen reached `main` without them.
The Skill's review (#94) raised both, ahead of Detail (#106), the first screen with a
navigation argument and a state to restore.

## Decision
A commit after the device pass that alters a screen repeats the pass for the states
it changed before the merge is requested: what was verified is what merges.

A branch that adds a screen also runs the exploratory pass over it; a branch that only
alters one runs the scenarios and states alone. A defect the branch's own change
causes is fixed in the branch, and any other confirmed defect becomes an app defect
issue (ADR 0048).

## Consequences
A pass now covered what merged, at the cost of a repeat after a fix that touched a
screen, usually a few states rather than the whole screen. A new screen's branch paid
about ten to fifteen more minutes of emulator time, for the checks that had caught
defects only after Search merged. The exploratory pass over a merged screen stayed as
it was.
