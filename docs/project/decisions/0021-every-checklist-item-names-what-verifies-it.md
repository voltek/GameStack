# ADR 0021 — Every checklist item names what verifies it today

Status: Accepted
Date: 2026-09-02
Backfilled: no
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Documentation integrity

## Context
ADR 0020 gave every decision an `Enforced by:` field on the argument that a rule
with no fitness function behind it is a wish. `DRIFT-CHECKLIST.md` was left
without the equivalent: its items said what drift to look for, and nothing said
who or what looked. That mattered because the checklist is the stated input to
the automated drift checker in `TOOLING-BACKLOG.md`, so sizing that work meant
re-deciding, item by item, which questions a tool could ever answer. The
estimate made without the field turned out to be wrong wherever it was checked —
four of the five *Documentation integrity* items had been called interpretive
and were string comparison with human triage.

## Decision
Every item in `DRIFT-CHECKLIST.md` carries exactly one `Verified by:` line,
naming what checks it *today* rather than what could check it one day. The
vocabulary is closed: `human`, `subagent (interpretive)`, `./gradlew lint`, and
`test: <TestName>#<caseName>` — spelled as `decisions/README.md` spells it, so
the two vocabularies join where they overlap.

`human` and `subagent (interpretive)` both mean a person does it now, and they
are not the same claim. `subagent (interpretive)` says the question needs
judgment to answer at all, so no test or script will ever settle it alone. That
distinction is what makes the field a coverage map rather than a status report.

## Consequences
The checklist becomes the automated checker's scope document: `human` is the
work it takes over, `subagent (interpretive)` is the work it never can, and a
value naming a tool is covered elsewhere already.

The first photograph was the finding, exactly as it was for `Enforced by:` —
one item verified by a tool, and every other item in this repository's audit
surface a person reading. That is the state this change recorded, not a target.

The cost, accepted knowingly: `human` collapses the mechanical and the merely
noisy into one value, so the field cannot say which of those items is a two-line
script and which is an afternoon of triage. A fifth value for the middle was
rejected because that distinction is a guess until someone builds the check, and
this repository has already been burned by writing down estimates that decayed.
