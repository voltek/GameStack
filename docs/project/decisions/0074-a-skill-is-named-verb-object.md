# ADR 0074 — A Skill is named verb-object

Status: Accepted
Date: 2026-11-12
Backfilled: no
Enforced by: none (prose only — .claude/rules/governance.md § Naming and describing a Skill) — the name's form and what a description holds

## Context
The Skills' names followed three patterns: verb-object (`clean-branch`, `open-pr`,
`write-tests`, and the generated `openspec-*`), `new-*` for the nine layer Skills,
and a few nouns. Anthropic's Skill authoring guide lists "inconsistent patterns
within your skill collection" among the names to avoid, recommends the gerund, and
accepts noun phrases and the action-oriented form. Two `new-*` names were false:
`new-api-service` forbade creating a service, and `new-repository-impl` also wrote
the interface. After #94 rewrote them, all nine descriptions said the Skill created
or changed its object.

## Decision
Name a Skill verb-object: a verb that covers creating and changing what it governs,
then its object, as `write-screen`, `build-feature` or `open-pr`. Its description
says what it does and when to use it, the key use first, in the imperative, and
names no Skill that invokes it by name.

## Consequences
The layer Skills became `build-feature`, `write-screen`, `write-viewmodel`,
`write-usecase`, `write-mapper`, `write-repository`, `write-igdb-endpoint`,
`write-hilt-module` and `write-room-storage`. The gerund was ruled out: the
generated `openspec-*` Skills cannot be renamed, and it would have renamed the
shipping Skills, terms in CLAUDE.md and many ADRs. `checkpoint` and `review-round`
kept their names, the first already a verb, the second the name of what it runs;
`discovery-feature` waited on #206, which decides whether it retires. The *Context*
of earlier ADRs kept the old names, since only their live fields changed.
