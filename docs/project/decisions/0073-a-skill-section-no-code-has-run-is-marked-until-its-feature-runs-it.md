# ADR 0073 — A Skill section no code has run is marked until its feature runs it

Status: Accepted
Date: 2026-11-12
Backfilled: no
Enforced by: none (prose only — .claude/rules/governance.md § Not exercised yet) — the marker's form, and its removal by the branch that runs the section

## Context
The Skill review (#94) wrote some sections ahead of the code they describe, such as
Room's Entity and write path and archiving an OpenSpec change, from the platform's
guidance, and marked each one as not yet run, naming Detail (#106). Nine markers
came out in three wordings, each repeating that the branch running it would correct
it. A reviewer flagged them as a story that goes stale, twice in one branch, and the
finding was dismissed both times: the text the reviewer read gave no reason for them.

## Decision
A Skill section that no product code has run yet carries `Not exercised yet (#n):`,
what does not exist, and one sentence telling the branch that first runs it to
correct it and remove the marker before its merge, where #n is the issue of the
feature that runs it first. The marker holds its instruction itself: a Skill run
through the Skill tool may not load a path rule, and some sections run after
`build-feature` step 9, in a review or at the merge.

## Consequences
The convention lived in `governance.md`, which loads when a Skill file is read, so a
reviewer opening one saw why the marker was there. Each marker became two short
sentences, and one spelling let `grep` find them all. The rule named no feature, so removing a marker
was left to the branch that ran its section, with no new ADR, and the rule covered
the next section written ahead of its code as well.
