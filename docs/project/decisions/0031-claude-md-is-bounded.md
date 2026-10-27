# ADR 0031 — CLAUDE.md is bounded at 200 lines and 18,000 characters

Status: Accepted
Date: 2026-10-26
Backfilled: no
Enforced by: none (prose only — CLAUDE.md § What belongs in this document — Tier 1)

## Context
CLAUDE.md had reached 612 lines and 35,950 characters, about 12,700 tokens,
loaded on every request and again in every subagent and review fork. Its own
"What belongs in this document" asked whether each addition was needed on every
turn, but set no ceiling. A trim on 2026-08-23 had stopped "deliberately rather
than hitting a line target", judging the rest to be rules; the file grew back.
Anthropic's guidance targeted about 200 lines for adherence, and the token cost
per request was a separate goal that a line count did not bound.

## Decision
CLAUDE.md holds at most 200 lines, for adherence, and 18,000 characters, about
6,000 tokens, for cost. Count characters as Unicode code points of the file as
committed, with LF line endings, so the count is the same on every checkout.

Until the file meets the bound, a change that adds text to it removes at least
as much in the same change, in lines and in characters; a rule that requires
the addition still applies, and the offset is how it is obeyed. Text that
leaves goes where it belongs: a procedure to its Skill, history to its record,
a decision's reasoning to its ADR.

Check both bounds in the build, as a Gradle task that `check` depends on, from
the first commit in which CLAUDE.md meets them. The gate stays the same three
commands.

## Consequences
The admission questions gained two numbers to answer to. A ratchet checking the
interim rule against the merge base was considered and declined: the changes
meant to meet the bound were planned next, and each would have had to lower it.
So the interim rule rested on prose and review. The cost, accepted knowingly: a
rule that needs more than a line or two now competes for space, and whatever
leaves is read only when its Skill or ADR is opened.
