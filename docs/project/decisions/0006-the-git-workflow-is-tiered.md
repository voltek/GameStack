# ADR 0006 — The git workflow is tiered, and the gate is all three green

Status: Accepted
Date: 2026-08-22
Backfilled: yes — DRIFT-LOG.md, row 2026-08-22 ("CLAUDE.md's Git workflow was the only section")
Enforced by: none (prose only — CLAUDE.md § Git workflow)

## Context
The Git workflow was the only section of CLAUDE.md with no Tier markers, so
nothing in it said what could be changed or by whom. It named a
"build/tests/lint gate" that it never defined anywhere, and it said nothing at
all about branching, untracked files, or PRs. Those gaps produced real damage
rather than theoretical risk: an untracked `ApicalypseRequestBody.kt` would have
been dropped by `git add -u` and silently broken the build for everyone else,
and a whole feature was carried to completion uncommitted.

## Decision
The section is tiered like every other section in the document.

Tier 1: the gate is all three green — `./gradlew build`, `./gradlew test`,
`./gradlew lint`; never `--no-verify`; never commit directly to `main`; read
`git status` in full before committing, because untracked source files are
invisible to `git add -u`.

Tier 2: branch names are `feature/`, `fix/`, `chore/` or `docs/{name}`; commit
messages follow Conventional Commits — `type(scope): imperative summary`,
lowercase, no trailing period. Conventional Commits is adopted as of this date
and earlier commits are deliberately not rewritten.

## Consequences
"Done" now has one definition that can be checked rather than remembered, and
the two habits that had already cost something — reading `git status` in full,
and not committing to `main` — are written where they are read every turn.

Everything here rests on prose. The mechanical half of the message rules is a
known gap with a home already: TOOLING-BACKLOG item 5, a `commit-msg` hook. The
gate's three commands later gained a mechanism of their own in `gate.yml`; the
rest of this ADR has none.

The cost of dating the convention rather than backfilling it: `git log` reads in
two styles either side of 2026-08-22, and that seam is permanent. Rewriting
merged history to hide it would cost more than the seam does.
