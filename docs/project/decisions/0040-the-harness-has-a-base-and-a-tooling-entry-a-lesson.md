# ADR 0040 — The harness has a base, and a tooling entry a lesson

Status: Accepted
Date: 2026-10-28
Backfilled: no
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Documentation integrity — one **Where.** and a **Replaces.** or **Ruled out.** per entry
Enforced by: none (prose only — TOOLING.md § What this file is, and what it is not) — the base, the mechanism that needs no named tool, no piece rebuilt only to drop a tool, whether a Replaces or Ruled out states a real lesson, whether Where names the file or setting
Enforced by: none (prose only — TOOLING.md § Rebuilding this elsewhere) — the copy generalised for export waits for a second project

## Context
TOOLING.md's admission was implied by its opening line, one entry per piece
naming the file that configured it, and nothing kept out an entry that only
described the state of the repository. Nothing said what the harness was meant
to run on. The owner set its reach: Android projects, later Kotlin
Multiplatform, built with Claude Code today, while the agent CLI, the forge and
the model might change. A **Scope.** line on every entry, naming the tools it
needed, was weighed over three review rounds and taken out, since each round
found a tool some scope missed; issue #91 holds it. Generalising TOOLING.md in
place was rejected for the reason issue #68 gave for the Skills: it lost the
concrete detail.

## Decision
The harness's base is a Gradle project for Android or Kotlin Multiplatform, with
git and a shell. Nothing is generalised past it. The forge, the agent CLI and
the model may change, so a piece takes the mechanism that needs none of them
when it costs about the same; otherwise it is built on the tool, and an adopter
translates it. No existing piece is rebuilt only to drop a tool.

An entry exists only for a piece configured by a file or setting it names under
**Where.**, and it states the step it replaced, a shape it ruled out, or both. A
state of the repository with no lesson is not an entry.

The copy generalised for export is made when a second project takes the
harness, not before.

## Consequences
An entry that only described the repository had no way in, and the Claude
workflows' entry, which named its files only in its What, gained a **Where.**
Which tools each entry needed stayed for the reader to judge, as before, until
issue #91's trigger, the same as the export copy's. Accepted knowingly: the base
and the principle were judged only by whoever wrote a new piece, and nothing
checked that a mechanism needing a named tool had really cost more.
