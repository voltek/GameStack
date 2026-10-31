---
name: checkpoint
description: Rewrite the session handoff (HANDOFF.md) and the open cycle (CYCLE.md) — after a merge or an abandoned branch, when a cycle item's status changes, and at session end. Holds the rules both files follow, the cycle's admission and breaker rules, and one task per session. Use whenever a cycle status changes or the session is ending.
---

## Why this exists
A session starts cold. The SessionStart hook (`.claude/hooks/session-start.sh`)
injects two small files and the repository facts it derives live, so a `/clear`
costs a few thousand tokens instead of re-reading the governance. Those files
only stay small and true if every checkpoint follows the same rules, and those
rules are here rather than in the files' own headers, which the hook paid for
on every session (ADR 0045).

## Where the files live
Outside the repository, in `$CLAUDE_HANDOFF_DIR`, or `~/Documents/<main
checkout's folder name>` when it is unset, shared by every worktree; the hook
prints the path it read. They are one person's live state: inside the
repository every pull request diff would carry it. A clone with neither file
still gets the derived facts, and a line saying no handoff exists.

## HANDOFF.md — session state only
- **Sections:** an `Updated:` line (the date and the event that triggered the
  checkpoint), then *Now*, *In flight*, *Owed by the owner*, *Constraints not
  written anywhere else*. *Now* ends with one `**Next action:**` line naming
  exactly one task, never a "then".
- **Never a *Next* list.** What comes next is `CYCLE.md`; anything not chosen is
  a GitHub issue (ADR 0038).
- **Rewritten whole at each checkpoint, never appended.** A done item leaves:
  its record is the commit, the ADR or the issue.
- **Bound: 80 lines and 6,000 bytes.** The hook warns when it is over, and its
  whole output is cut to a 2,000-character preview past 10,000 characters.
- **Nothing derivable:** branches, hashes, the next ADR number, counts. The hook
  prints those live, and a stored copy goes stale.
- **If it contradicts the repository, the repository wins.** Flag the
  discrepancy to the owner before continuing.

## CYCLE.md — one finite cycle
- **Chosen at a triage** (ADR 0038, `ship-a-branch` § Then check whether a
  triage is owed), with a *Chosen*, a *Goal* and an *Exit* line, then a table of
  items: number, item, what it points at, clause, status. Items point at an
  issue or a plan; they never copy its tasks.
- **Only statuses change.** An insertion is appended to the *Insertions* table
  with its clause.
- **Admission, for an item or an insertion:** (1) it blocks the next product
  step; (2) it is a defect on `main`; (3) done first, it avoids rework on the
  next item; (4) the owner names it. Anything else is an issue.
- **Breakers.** A third insertion in one cycle stops it: re-triage. An item that
  overruns — a third review round, the rule of three — goes back to the pool at
  the next triage. The hook warns past 30 lines: a growing cycle is a breaker.
- **Closed whole** when the exit condition holds: move the file to
  `archive/cycles/`, then choose the next cycle at a triage from the issue pool.
  Nothing unchosen carries over.

## One task per session
A task is one branch, and its session runs it to the merge or the abandonment:
evaluation, review rounds, triage, pull request, merge. The session ends there —
checkpoint and stop, even when told to continue. Never start a second branch: a
handoff that chained two tasks led an agent to start the second in the same
session, filling its context with unrelated work.

## When to checkpoint
When a `CYCLE.md` status changes (a merge, a branch abandoned, an insertion,
the cycle closing) and at session end. Not per commit: `git log` and the hook
already show those.

## Steps
1. Run the hook by hand from the repository root, for the live facts and the
   bound warnings: `bash .claude/hooks/session-start.sh </dev/null`.
2. Update `CYCLE.md`'s statuses; close the cycle if its exit holds.
3. Read `HANDOFF.md` with the Read tool, then rewrite it whole with Write.
4. Check the bounds on the handoff at the path the hook's
   `== HANDOFF.md (<path>) ==` header prints: `wc -l` and `wc -c` within 80
   and 6,000, and no hash or next ADR number anywhere:
   `grep -E '\b[0-9a-f]{7,40}\b|next (free )?ADR' "<path>"`.
5. Route what the session learned, never into the handoff: a preference to
   agent memory, a rule change proposed to the owner, a decision to an ADR, an
   incident to the commit body, pending work to an issue.
6. At session end, stop. Report in three lines or fewer: what changed, the next
   action, anything owed by the owner.

Archive conventions (review records, done plans) and a handoff template:
(PENDING — not yet defined; #87 part B).
