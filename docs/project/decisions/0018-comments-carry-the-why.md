# ADR 0018 — Comments carry the why, never the what and never the history

Status: Accepted
Date: 2026-08-23
Backfilled: yes — DRIFT-LOG.md, row 2026-08-23 ("The 2026-08-22 entry below already stated the rule")
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Code Conventions

## Context
The placement rule already existed — a generalizable rule lives in CLAUDE.md
plus its Skill and gets a pointer in code, rationale for one non-obvious line
stays in the code, never both — and it was then broken five times in one file.
Measured: `SearchViewModel.kt` at 25% comment lines and `SingleClick.kt` at 35%,
against 5–9% everywhere else in the project, one sediment layer per review
round. Most of the excess was *history* — which earlier version carried which
defect, which review found it — duplicated from the drift log, and some of it
would be false after the next refactor. This is a known failure mode of writing
code with a model: design decisions get narrated into comments, and the words
are free to produce and costly to read.

## Decision
Three questions before leaving a comment:

1. Does it restate what the code already says? Delete it.
2. Is it about *how we got here* rather than *what to do here*? That belongs in
   the commit message and the drift log, both of which this project maintains —
   and the copy in the code is the one nobody updates.
3. Would it become false if someone refactored correctly? Then it is a
   chronicle, not a constraint.

Prefer encoding the why as a name, a type, or a **test** — a test states the
invariant *and fails when it breaks*, which no comment can do.

Bounds, as smoke alarms rather than limits: no comment block longer than 4 lines
in production code, and ~15% comment lines in a file is worth looking at. The
percentage is meaningless below roughly 50 lines.

## Consequences
`SingleClick.kt` went from 35% to about 14% with nothing lost — every deleted
line already existed in a Skill or in the drift log. `SearchViewModel.kt` went
from 25% to 16% and was deliberately *not* forced below the alarm, because
cutting a comment that earns its place to hit a number is the failure mode this
rule itself warns about.

What survives such a prune is the useful half: the traps a correct-looking
refactor would walk into. What goes is the narration.

The cost: judgement is required per comment, and the two numbers are heuristics
that will occasionally point at a file that is fine. The three questions are the
actual rule; the bounds only say where to look first.
