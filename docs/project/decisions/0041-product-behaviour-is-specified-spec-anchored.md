# ADR 0041 — Product behaviour is specified spec-anchored, in OpenSpec's format

Status: Accepted
Date: 2026-10-29
Backfilled: no
Supersedes: ADR-0038 — "its OpenSpec item included" (Decision)
Enforced by: none (prose only — .claude/skills/ship-a-branch/SKILL.md § A branch with a change verifies it before the reviews, archives it before the merge) — which branches open a change, verify before the reviews and archive before the merge, a verifier following its renamed test, no open change left on a merged branch
Enforced by: none (prose only — openspec/schemas/gamestack/schema.yaml § artifacts) — the `VERIFIED BY` lines on every scenario, the proposal's `## Assumptions` and `## Open Questions`, tasks grouped by layer Skill
Enforced by: none (prose only — docs/project/TOOLING.md § 13. Spec-driven changes) — the generated Skills never edited, the machine profile, the pilot's verdict after Detail

## Context
The Spec held each screen's behaviour as a few bullets, written before the code.
Search's initial state and its refresh-failure banner were found by review after
the build, not by the spec, and the Spec recorded them afterwards together with
how they were found. Detail had four bullets and no states, while two Skills
called it fully specced. Spec Kit, Kiro, Tessl and OpenSpec were compared; only
OpenSpec kept specs alive across changes with two or three files per change and
its own CLI rather than an IDE or a second constitution beside CLAUDE.md.

## Decision
What the user sees is specified per capability in
`openspec/specs/<capability>/spec.md`, as requirements with WHEN/THEN scenarios,
in OpenSpec's format. A spec's behaviour changes only through a change,
`openspec/changes/<branch-kebab-name>/`: a proposal, a delta spec and tasks, and
a design only for a decision local to that change. Every scenario names what
proves it: a test by its class and name, or the device pass. A test is a
reference, not behaviour: the commit that renames or moves one updates every
scenario naming it, on any branch.

A `feat` branch that adds or changes what the user sees opens a change. A `fix`
branch opens one when the bug was unspecified behaviour, its scenario what the
regression test pins, or when the spec itself specified the wrong behaviour.
Other types open none. A change is verified before its branch's reviews and
archived after the last of them, the GitHub bot's when it runs, before the
merge. Nothing is converted in bulk: a capability gets its spec from its first
change. Search's spec, written with this decision as its worked example, is the
one exception.

A proposal lists its assumptions and its open questions. The owner accepts or
overrides each assumption when approving it, so an assumption is a proposal,
not a default taken; a change with an open question is not approved.

OpenSpec's CLI and generated Skills are piloted on Detail's spec, customised
only through `openspec/config.yaml` and `openspec/schemas/gamestack/`, and never
edited. After the pilot the owner keeps them, or keeps only the format.

## Consequences
Behaviour gained one home per capability, read on demand and changed with the
code that implemented it, and the review of a change could check it scenario by
scenario. Search's spec was written from the Spec and its tests as the worked
example. Accepted knowingly: a Node CLI on every machine that runs a change, a
per-machine profile, seven Skill descriptions in every turn's listing, and
generated files that `openspec update` rewrote. `openspec validate` stayed out of
the gate until the pilot's verdict.
