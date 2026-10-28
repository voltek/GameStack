# ADR 0037 — The comment bound is a limit in every hand-written file

Status: Accepted
Date: 2026-10-27
Backfilled: no
Supersedes: ADR-0018 — "Bounds, as smoke alarms rather than limits: no comment block longer than 4 lines in production code" (Decision) and "the two numbers are heuristics that will occasionally point at a file that is fine" … "the bounds only say where to look first" (Consequences)
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Code Conventions — the 4-line limit and its scope, placement next to the line, the 15% alarm
Enforced by: none (prose only — CLAUDE.md § Code Conventions) — where a longer rationale goes

## Context
ADR 0018 bounded a comment block at 4 lines "in production code", as a smoke
alarm, and never said what that covered. A review finding on a 24-line block in
`gate.yml` was dismissed twice on the ground that a workflow is not production
code, and nothing in the text could settle it; an alarm could have been
dismissed on any ground. The number itself held: 124 of 126 Kotlin blocks and
every build script were already within it. What broke it was one preamble
guarding three separate lines of a workflow, which had no test to encode an
invariant in.

## Decision
A comment block holds at most 4 lines, a limit rather than a smoke alarm, and
sits next to the line it constrains. This applies to every hand-written file
except documentation: code in main and test, build scripts and configuration
(`*.kts`, `libs.versions.toml`, `*.properties`, XML), workflows, hooks and
scripts. Generated or template text is exempt: `gradlew`, `gradlew.bat`, and
comments left by Android Studio or an app install. The warning itself stays
inline. A reason that needs more goes to a test that guards it, the ADR,
TOOLING.md for a workflow or the owning Skill for a procedure, named in a
one-line pointer, or to the commit body, which `git blame` on that line finds.
The 15% comment-line figure stays a smoke alarm.

## Consequences
The scope answered the reviewer's question in the text, and a limit left no
ground to dismiss it on, which is what stops a recurring false positive. Three
blocks became violations and were cut in the same change: two in `gate.yml`
and a `SearchViewModelTest` regression note. `MessageState.kt`'s scroll note
already broke ADR 0018 and was fixed as drift on its own. Splitting a preamble
into notes beside the lines they guard put each warning where an editor of that
line reads it. The cost, accepted knowingly: a workflow's full reasoning lives
in TOOLING.md, one file away from the line it explains, and a comment that
would earn a fifth line has to move its reasoning out.
