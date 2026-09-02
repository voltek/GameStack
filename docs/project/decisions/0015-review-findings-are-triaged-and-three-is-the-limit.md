# ADR 0015 — Review findings are triaged, and the rule of three stops the patching

Status: Accepted
Date: 2026-08-23
Backfilled: yes — DRIFT-LOG.md, row 2026-08-23 ("Nothing recorded how review findings get triaged")
Enforced by: none (prose only — CLAUDE.md § Git workflow Tier 2)

## Context
Nothing recorded how review findings get triaged, when a review loop is allowed
to stop, or what happens when the automated reviewer is unavailable. In practice
PR #3 ran five bot rounds plus one `/code-review` in a single day, hit the Codex
quota, and stayed open long enough to accumulate work unrelated to its own unit.
Nothing recorded the habit that made its regression tests worth anything either
— verifying that each one fails against the code it guards.

## Decision
Every review finding is fixed or dismissed with a written reason, and every
thread is answered and then resolved.

A round producing only design suggestions means merge; a round producing a real
defect earns another round. Automated review is an advisor, requested once per
PR, and may be skipped when unavailable. `/code-review` is run by the human — the
agent cannot invoke it, and never ticks that box on its own behalf.

**The rule of three:** touching the same code a third time for the same class of
defect means the design is the problem, not the instances — stop patching and
redesign. A fix that introduces the next defect in the same place counts double.

And every regression test must be **seen to fail** against the code it guards.
A test written after the fix can pass whether or not the fix is there, which
makes it worse than no test: it looks like coverage and proves nothing.

## Consequences
A review loop now has a defined stopping condition, so it ends on a judgement
rather than on exhaustion or a quota.

The rule of three is the part that has repeatedly paid: the Search pipeline was
redesigned rather than patched a fourth time, and the refresh-error mechanism
after that, both on this rule.

The cost: "the same class of defect" is a judgement call, and the rule fires
late by design — it is a stopping condition, not a preventive one.

Nothing mechanical enforces any of it. The checklist carries a detector for the
regression half — did a regression test land without being seen to fail — and
automating that check is TOOLING-BACKLOG item 1. The triage and the rule of
three are read, argued and remembered.
