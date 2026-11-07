# ADR 0053 — Every line in `.claude/rules/` answers the admission questions

Status: Accepted
Date: 2026-11-06
Backfilled: no
Supersedes: ADR-0050 — "and a \"Prefer\" heading marks rules that may be challenged with a stated reason" (Decision)
Enforced by: none (prose only — .claude/rules/governance.md § What earns a line in CLAUDE.md) — the questions binding `.claude/rules/`, the source question

## Context
ADR 0050 asked each line in CLAUDE.md to earn its place, and ADR 0051 moved rules to
`.claude/rules/` by asking where they belonged, not whether they should exist. A rule
that loaded by path escaped the test, and it would still be read in its area and copied
by every clone.

A review of about 60 rules before the architecture sections moved found rules that were
stale, restated a Skill or contradicted another rule. `UiText` was required in UseCases,
which cannot import it; one class per file failed on `NetworkQualifiers.kt`, whose form
a Skill prescribed. That review's first draft recalled good practice from memory, and
checking it later showed ADR 0011 against Android's UI events guidance.

## Decision
The admission questions bind every line of `.claude/rules/`, including what is already
there, and moving a line is no reason to keep it.

A sixth question joins them: does the source support the rule? The platform's guidance
is read rather than recalled, and a rule that departs from it on purpose says so.

CLAUDE.md's architecture, data and convention sections leave. What survives lives in a
rule file by area: `code.md`, `comments.md`, `data-layer.md`, `navigation.md` and
`gradle.md`, with `presentation.md` and `tests.md`. A rule a Skill already holds stays
only there, and a mechanical rule is marked as a Konsist candidate until #163 covers it.
The "Prefer" sentence leaves, since no rule sits under such a heading any more.

## Consequences
CLAUDE.md fell from 315 lines to 49, and the rule files shrank too. A rule that left
because its Skill held it was relocated rather than questioned: the review of every Skill (#94)
owes those the same questions.

A review reading a diff through git saw none of the moved rules, the gap of ADR 0051,
which #168 tracks; no code branch was due before Detail, so it waited for #99.

ADR 0011 stayed in `presentation.md`, shortened, pending #175. Rules for Result, the
toolchain and Detekt went to #174, #173 and #176. Nothing here enforced the questions.
