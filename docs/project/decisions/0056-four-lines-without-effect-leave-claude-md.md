# ADR 0056 — Four lines measured without effect leave CLAUDE.md

Status: Accepted
Date: 2026-11-06
Backfilled: no
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Code Conventions — code and comments in English

## Context
ADR 0050 kept several lines on judgement after their comparison was lost to the usage
limit. A batch on 2026-11-06 ran seven of them: 28 runs on `claude-sonnet-5-5` at `high`
effort, two per side, in a fresh clone each, with the method of TOOLING.md § 7.

Four lines showed no effect. Without them the agent still branched before committing,
updated a pushed branch with `git merge main` and proposed an ADR before waiting, and
answered a Spanish prompt with an English comment. Purpose and the check of a library's
version changed what the agent did, so both stayed. The comparison on comments said
nothing, because its task never tempted the agent to narrate history.

## Decision
The lines on committing on `main`, on `git merge main`, on how a rule changes and on
English leave CLAUDE.md, and each rule stays in force. `git merge main` is written in
`ship-a-branch` § 1 and English in `DRIFT-CHECKLIST.md`.

The other two are stated here, and no line tells an agent either of them. Never commit
directly on `main`: Claude Code's own guidance and the line on one branch per unit of
work lead an agent to branch, and branch protection stops a commit reaching `main`. A
rule changes only with the owner's agreement, through an ADR: Ask first covers the
agreement, and no ADR reaches `main` without the owner's merge.

A line that returns needs a run or a review in which an agent broke its rule.
`.claude/rules/comments.md` stays on judgement until a task tempts the agent.

## Consequences
CLAUDE.md fell from 49 lines to 44. ADR 0055's bound stayed, so the file gained room
rather than a tighter limit.

Two runs per side show only large differences, and on one model. Repeating them on a
second model was ruled out: each result would cost double, and a wrong removal shows in
a review and returns in one commit. The run on how a rule changes was the weakest,
because it asked what the agent would do rather than letting it act. The effort was
inherited rather than chosen, which #177 addresses.
