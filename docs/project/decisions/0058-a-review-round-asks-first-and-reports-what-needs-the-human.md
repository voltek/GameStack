# ADR 0058 — A review round asks first and reports what needs the human

Status: Accepted
Date: 2026-11-09
Backfilled: no
Supersedes: ADR-0049 — "fixed, deferred to an issue, or dismissed", "before anything changes", and "On a branch that is wholly governance the human decides the rows that turn on a judgment, and the others stand unless reopened" (Decision)
Enforced by: commits.yml — the review record's `Accepted:` line free of `TODO`
Enforced by: none (prose only — .claude/skills/review-round/SKILL.md § Before each round, ask) — asking before every round, an answer with a question or a request not counting as a yes
Enforced by: none (prose only — .claude/skills/review-round/SKILL.md § The sequence) — the order from the raw output to the verdict, the verdict before any push or PR
Enforced by: none (prose only — .claude/skills/review-round/SKILL.md § Buckets) — `Accept`, a deferred defect saying what it breaks
Enforced by: none (prose only — .claude/skills/review-round/SKILL.md § Verifying) — a code defect whose test does not fail going back to the human
Enforced by: none (prose only — .claude/skills/review-round/SKILL.md § The message) — the table only in its file, the rows the chat shows on every branch
Enforced by: none (prose only — .claude/skills/review-round/SKILL.md § A partial answer) — confirmed rows applied while another is open
Enforced by: none (prose only — .claude/skills/review-round/SKILL.md § Which branches get a round) — the `.gitignore` exemption
Enforced by: none (prose only — .claude/skills/open-pr/SKILL.md § 5. The GitHub bot's round) — the bot's 30 minutes, counted from the PR's opening, no local round after a fix to its findings

## Context
Issue #184 found the steps after a review scattered across `ship-a-branch` § 3
and § 4, with three of them only in agent memory: no round while a question
stood open, the raw text copied from the subagent's notification, and the
verdict said before any push. Finding ids, the chat's form and what verifying a
`Fix` meant varied from round to round. `Dismiss` was meant for a wrong
finding, yet PRs #158 and #186 recorded valid findings the owner accepted under
it, with reasons that said why a fix was not worth it rather than why the
finding was wrong. The owner confirmed nine rows of a table and asked about the
tenth, and the rule read as "touch nothing". The agent had launched rounds while
the owner still meant to change the branch. A one-line `.gitignore` branch
shipped without review on the owner's word, since no exemption covered it
(#151). The bot's 30-minute cutoff decided whether a PR merged without it, and
no ADR held it (#37).

## Decision
Before every round, say what it reads and what stays open, and launch it only
on an explicit yes; an answer that asks or requests something is not one.

Triage every finding into a fix, a deferral to an issue, an acceptance, or a
dismissal. An accepted finding is valid, not a defect and not worth fixing; it
opens no issue, only the human accepts it, and a finding that can cite a
failure scenario is never accepted. A code defect whose regression test does
not fail goes back to the human.

Keep the table in its file. On every branch the chat shows the rows the human
decides, the fixes that change more than the finding asked, undo the human's
call or touch a file outside the branch's unit of work, and what leaves the
branch; the others stand unless reopened. Rows the
human confirmed may be applied while another is open, unless they share its
section; nothing is committed, folded, reviewed again or opened as a PR before
every row and the verdict are confirmed.

A diff that only adds literal paths to `.gitignore`, and leaves no tracked file
ignored, needs no review round.

The GitHub bot's round is unavailable 30 minutes after the PR opens with no
result. No local round reads a fix to its findings; each is fixed as no round
will read it.

## Consequences
One sequence in `review-round` replaced clauses spread across a Skill, memory
and an ADR, and a clone read the three rules that had lived only in memory.
`Accept` separated a reviewer's error from a cost the owner chose, which kept
the false-positive rate readable, and it gave cheap valid findings an end other
than a `review-debt` issue. It cost one more bucket, a record line and its
check.

Product branches stopped showing every row: the agent chose what the owner saw
there too, measured only by the first product branches to try it, and the owner
could always ask for the whole table. Applying confirmed rows early risked work
the open row undid, which the shared-section exception bounded. Asking before
every round added a message per round.

A literal `.gitignore` path could still ignore a file nobody had created yet;
the check on tracked files covered only those that existed.
