# ADR 0055 — CLAUDE.md is bounded at 60 lines and 3,600 characters

Status: Accepted
Date: 2026-11-06
Backfilled: no
Supersedes: ADR-0031 — "at most 200 lines", "18,000 characters, about 6,000 tokens", "Until the file meets the bound" and "as a Gradle task that `check` depends on" (Decision)
Enforced by: test: ClaudeMdBoundTest#should keep CLAUDE md within its line bound — the line bound
Enforced by: test: ClaudeMdBoundTest#should keep CLAUDE md within its character bound — the character bound

## Context
ADR 0053 left CLAUDE.md at 49 lines and 2,857 characters. ADR 0031's bound of 200 lines
and 18,000 characters was then more than four times the file, so it could regrow that
far with nothing firing, and it had regrown before. Anthropic's guidance only asks for
under 200 lines, a ceiling the file already met, so no official figure set a lower one.

## Decision
CLAUDE.md holds at most 60 lines and 3,600 characters: its size when it first met the
bound, plus about 25%. The figure is this project's, not official. ADR 0031's way of
counting stays. A unit test checks both, so the gate stays three commands and needs no
new `Enforced by:` value, and its message names the reason and where text should go.

## Consequences
A new line that does not fit forces the question of what leaves, which a far-off bound
never did. The cost is a test that reads a document, which Gradle had to be told about
or it stayed UP-TO-DATE while the file grew, and a margin small enough that a
real need may have to raise it through a new ADR. Any edit to CLAUDE.md reruns the unit
suite on both variants, about 13 seconds each, which the file's rare edits make cheaper
than a task of its own in the gate. Line count stopped being the only
guard on adherence: the admission questions still decide what stays.
