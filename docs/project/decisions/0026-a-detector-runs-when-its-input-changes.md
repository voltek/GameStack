# ADR 0026 — A detector runs when its input changes

Status: Accepted
Date: 2026-10-13
Backfilled: no
Supersedes: ADR-0022 — every *Documentation integrity* detector runs on every governance diff
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Documentation integrity — the shape of each `Runs when:` line and that its paths exist
Enforced by: none (prose only — DRIFT-CHECKLIST.md § Documentation integrity) — which detectors a diff runs, and whether a `Runs when:` names every path its detector reads
Enforced by: none (prose only — DRIFT-CHECKLIST.md § Adding or changing a detector) — a new or changed detector seen to fire
Enforced by: none (prose only — decisions/0026-a-detector-runs-when-its-input-changes.md § Decision) — no course or study vocabulary in the repository

## Context
ADR 0022 ran every *Documentation integrity* detector on every governance diff.
On the branch for ADR 0025 that meant either reading DESIGN.md, which the diff
did not touch and so could not have changed, or skipping it and reporting the
section as run. The course-vocabulary detector had been written on 2026-08-23
as the fix for drift a person found, and had found nothing since. And the
detectors themselves had been mutation-tested on two branches, but no document
asked for it, so the next session would not have.

## Decision
Split the section in two. *Every run* holds the detectors whose input is every
document — references to undefined things, decisions taken without being written
down, which a diff touching only documents can still unwrite — or nothing in the
repository at all: GitHub's settings, which change on GitHub with no diff.
*When the diff touches what it reads* holds the rest, each with a `Runs when:`
line naming the paths it reads; it runs only when the diff touches one of them.

Retire the course-vocabulary detector, not its rule: documents name the work
itself, never course or study vocabulary ("Block N", phase numbering, learning
objectives), which belongs to the course kept outside this repository.

A detector that is added or changed is seen to fire before it is trusted: one
planted defect per thing it claims to catch, in a copy, each reported.

## Consequences
A governance diff ran less of the checklist and said which conditional items it
skipped, so a skip was a recorded decision rather than a silent one.

The cost, accepted knowingly: a `Runs when:` line that missed a path a detector
reads would hide drift until another diff touched it, and a path list had to be
kept true by hand until the detectors became tests. The course-vocabulary rule
had lived nowhere but its detector, so from then on it was written only here, and
nothing checked it.

Both halves were written for detectors a person runs. A detector turned into a
JVM test would run on every gate whatever the diff touched, and CLAUDE.md's
regression-test rule would already require it to be seen to fail.
