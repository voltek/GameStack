# ADR 0014 — The force-push window closes at the merge

Status: Accepted
Date: 2026-08-23
Backfilled: yes — DRIFT-LOG.md, row 2026-08-23 ("Two workflow rules were producing the mess")
Supersedes: ADR-0007
Enforced by: none (prose only — CLAUDE.md § Git workflow Tier 2)

## Context
Two workflow rules were producing the mess they were written to prevent. PRs
were being opened before the `/code-review` rounds finished, so threads anchored
to SHAs that later moved, the force-push window closed early, and the PR body
went stale from the day it was written. And ADR 0007's window — closing at the
*first review* — froze branch history in order to protect bot threads: the
`chore/screen-tests` branch reached eight commits, four of which existed only to
correct earlier commits on the same branch, which CLAUDE.md already forbade with
no point of enforcement.

Weighed properly, that trade was backwards. Branch history is permanent and
drives `blame` and `bisect`; a review thread is read once and effectively never
revisited after the merge.

## Decision
The force-push window closes at the **merge**, not at the first review.
`--force-with-lease` is permitted on your own feature branch until then, never
bare `--force`. Reply in the thread before rewriting — the reply is the history,
the anchor is not.

Alongside it, and for the same reason: every review round runs **before** any PR
exists, and a branch is shaped into one commit per claim, folding in every
correction of not-yet-landed work.

The absolute bans are untouched: never `main`, never a branch another person may
hold, never bare `--force` anywhere.

## Consequences
Orphaning a review thread's diff anchor is now an accepted cost rather than a
blocker, and a branch's history can be made true before it becomes permanent.

The loss was measured rather than argued, on PR #12: after a force-push the
thread keeps its text, its `path` and its `originalLine`, loses `line` (the live
anchor becomes null) and is flagged `isOutdated` — and it still accepts replies
and still resolves. Resolving it moved that PR from `BLOCKED` to `CLEAN`, so the
`main` protection requiring conversation resolution cannot deadlock. What is
lost is the presentation, not the conversation.

The cost: a wider window is a wider chance to rewrite something that should have
stood, and the rule leans on the absolute bans to keep that survivable.

`Enforced by` names CLAUDE.md because that is where the window itself lives. The
other half of this decision — reviews before the PR, one commit per claim — is a
procedure, so it lives in `ship-a-branch`, and one field cannot address both.
