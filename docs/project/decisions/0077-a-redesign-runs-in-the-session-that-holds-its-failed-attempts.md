# ADR 0077 — A redesign runs in the session that holds its failed attempts

Status: Accepted
Date: 2026-11-12
Backfilled: no
Enforced by: none (prose only — .claude/skills/review-round/references/past-the-cap.md § Where a redesign runs) — the redesign kept in the session that holds the attempts, or in the branch that needs it when one was made elsewhere

## Context
When the rule of three fired, the redesign ran on the session's model and effort,
after the failed fixes, with nothing saying where it ran (#194). #194 proposed
handing it to a read-only subagent on the most capable model, briefed with the
attempts. Claude Code's subagent documentation keeps work in the main conversation
when its phases share significant context, as planning, implementation and testing
do, and a subagent does not see the conversation's history. Measured (#194's
comment of 2026-11-12), the subagent's first request was 6,386 tokens, but the
session still read the same files to implement its design.

## Decision
A redesign runs in the session that holds the failed attempts. When an earlier
touch was made in another branch, the session reads it from that branch's history:
if this branch needs the defect gone, the redesign is this branch's one redesign;
if not, it is an issue, and this branch leaves that code as `main` has it.

## Consequences
The redesign kept the attempts and the reasoning behind them, so it did not retry
what had failed, and a defect the branch needed gone did not merge on the strength
of an issue. The subagent was ruled out: a brief carried a summary of the attempts
where the session held them whole. So was the advisor pattern, a stronger model
consulted by the session's: in Anthropic's own trials, cited in #194, none of four
pairings clearly beat its executor alone. A step up of effort or model for the
redesign was left to the owner's judgment rather than proposed by rule: each switch
can rewrite the prompt cache of the whole conversation, and stepping back down pays
it again.
