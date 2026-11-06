# ADR 0051 — Rules for one part of the code load by path

Status: Accepted
Date: 2026-11-05
Backfilled: no
Supersedes: ADR-0009 — "a rule that applies to future code lives in CLAUDE.md plus its owning Skill" (Decision)
Supersedes: ADR-0038 — "its rule to CLAUDE.md" (Decision)
Supersedes: ADR-0043 — "How the app is built stays in CLAUDE.md" (Decision)
Enforced by: none (prose only — .claude/rules/governance.md § What earns a line in CLAUDE.md) — a rule that binds one part of the code goes in `.claude/rules/`

## Context
After ADR 0050, CLAUDE.md still exceeded ADR 0031's bound. Testing Stack, Device
verification and Accessibility bound one kind of file, yet they loaded on every
request and in every review fork. Commands repeated the gate's line. Available
Skills repeated what the harness lists, with three rules buried inside the list.
Probes on 2026-11-05, on Claude Code 2.1.318 with one run each, showed a rule with
`paths:` reaching the agent on a Read, on `cat` through Bash and in a subagent that
read a matching file. On a new file it arrived with the first Write's result, after
the draft, and Grep never loaded it.

## Decision
A rule that binds one part of the code lives in a `.claude/rules/` file with `paths:`.
`tests.md` covers the test source sets and the build files naming the test stack, and
`presentation.md` covers the UI code, its resources, the manifest and the mockups. Both
cover OpenSpec changes, which specify what those rules bind.

Commands and Available Skills leave CLAUDE.md. The list's three rules go their own
ways: never editing a generated Skill is a `deny`, with a line in `governance.md`
because a pattern misses other spellings; a visible `feat` starting from a change is
announced by the `new-feature` Skill's description and repeated in `presentation.md`;
and "a task with no Skill is a gap to name" is dropped.

## Consequences
A path rule reached only a session that touched its area, so a session shipping a UI
branch without opening one relied on the PR template's device-pass box. A review
reading a diff through git loaded no rule, as a fresh session showed, so a round saw
one only by opening a matching file; this branch deferred that to issue #168. The
Skills-list detector left with the list, and its retired-Skill clause stayed as a
detector of its own.

Fresh sessions confirmed each path, and a file outside them loaded no rule. A rule file
created during a session did not reach that session.

Comparisons on `claude-sonnet-5-5`, three runs each way, settled what stayed. A line
telling an agent to read a file in the area before writing one left: all six read one
regardless. "Behaviour lives in `openspec/specs/`" stayed out of CLAUDE.md: all six
updated the spec for a change in `core/data`. For a feature starting in the data layer,
the plan hedged on opening a change in all three runs by path. A line in CLAUDE.md and a
fuller `new-feature` description each made it a firm step, and a rule on
`app/src/main/**` did not load in time. The description won: it restated step 0, and
sessions building no feature had no use for a line. Those runs compared plans only.

"A task with no Skill" left because the official test for a Skill is repetition, not
every task, and one commit wrote it and no Skill cited it. "Prefer invoking" Skills left
too, since the Skill tool already ordered it.

The `deny` was tried on 2026-11-05: Write, Edit and a `touch` through Bash in that
folder were refused, and an agent's `openspec update --force` under it rewrote the
Skills with no denial, so an upgrade still worked. The moves dropped the Robolectric
timings and the count of Search defects, which git history keeps.
