# ADR 0005 — A UI change is verified on the emulator before a merge is requested

Status: Accepted
Date: 2026-08-22
Backfilled: yes — DRIFT-LOG.md, row 2026-08-22 ("CLAUDE.md defined")
Enforced by: none (prose only — CLAUDE.md § Commands)

## Context
CLAUDE.md defined "done" as `build`, `test` and `lint` green, so a UI feature
could be declared finished with no device check at all — and none was ever
proposed until the human asked for one. Every UI defect of that session was
invisible to the gate by construction: a premature empty state, a keyboard
covering a button, clipped genre text. The gate proves the code compiles and
that logic does what a test asserts; it can never prove a screen looks right or
that a control is reachable. The technique for running the check was unrecorded
too — it lived in one session's memory, where a fresh agent on another machine
would have had to rediscover it.

## Decision
Every change that adds or alters a screen is verified on the emulator before a
merge is requested, covering every state that screen declares, and the PR body
says what was checked. Data or domain work with no UI change does not need the
pass.

The emulator is *asked for*, never launched unprompted. The choice between a
`uiautomator` dump and a screenshot follows from the question being asked, and
the dump's IME blind spot is stated where the procedure lives. The procedure
itself is the `verify-on-device` Skill, not CLAUDE.md: it is a sequence followed
while doing one named task, and CLAUDE.md is paid for on every turn.

## Consequences
"Done" for UI work now includes the only check that can catch the defects the
gate is structurally blind to.

The cost is real and accepted: the check is manual, it needs a running emulator,
and it is the slowest step in shipping a screen. It is also the step with no
mechanism behind it — nothing fails if it is skipped, which is why the PR body
has to say what was covered. That statement is the whole enforcement.
