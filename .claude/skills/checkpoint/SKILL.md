---
name: checkpoint
description: Rewrite the session handoff (HANDOFF.md) and the open cycle (CYCLE.md) — after a merge or an abandoned branch, when a cycle item's status changes, and at session end. Holds the rules both files follow, the cycle's admission and breaker rules, when a triage is owed and what it proposes, and one task per session. Use whenever a cycle status changes, a triage is owed, or the session is ending.
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
still gets the derived facts, and a line saying no handoff exists. The cycles
are kept on one machine, the one with a `CYCLE.md` or an `archive/cycles/`
beside the handoff; a shared home for a team's cycle is #134's question.

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
- **Chosen at a triage** (§ Triage), with a *Chosen*, a *Goal* and an *Exit*
  line, then a table of items: number, item, what it points at, clause, status;
  then an `## Insertions` heading over an empty table of the same columns.
  Items point at an issue or a plan; they never copy its tasks. A triage writes
  it within the line bound below, or the breaker fires on a new cycle.
- **Only statuses change.** An insertion is appended to the table under
  `## Insertions` as a row with its number and its clause: the hook counts every
  row of that table, and owes a triage when the heading is missing.
- **Admission, for an item or an insertion:** (1) it blocks the next product
  step; (2) it is a defect on `main`; (3) done first, it avoids rework on the
  next item; (4) the owner names it. Anything else is an issue. A `product`
  issue admitted takes Horizon Now, which holds only the open cycle's product
  issues (ADR 0047).
- **Breakers.** A third insertion in one cycle stops it, and so does the file
  growing past 30 lines. An item that overruns — a third review round, the rule
  of three — goes back to the pool at the next triage.
- **Closed whole** when the exit condition holds, or **stopped** when a breaker
  fires: either way, move the file to `archive/cycles/`, and a triage chooses
  the next cycle from the issue pool. Nothing unchosen carries over. Closing is
  that move: the hook reads a `CYCLE.md` that is present as an open cycle.

## Triage
- **Owed** when no cycle is open and when a breaker fires (ADR 0047), never by a
  merge alone, and only where the cycles are kept: elsewhere the hook says no
  cycle is kept there, and nothing blocks a branch. The hook prints `TRIAGE
  OWED` with its reason, first after any bound warning, and lists the open
  issues that carry a bucket but no priority, neither `gated` nor `product`. An
  owed triage runs before the next branch starts.
- **What it proposes** to the owner, who decides: what ADR 0038 § Decision lists
  for a triage, for every open issue but the `Triage log` and the `product`
  ones. Each ungated issue's priority, proposed when it opened, is confirmed or
  changed. Then the next cycle (§ CYCLE.md), its items admitted by the clauses.
- **Product issues** (ADR 0043): read `PRODUCT.md`'s *First release*, *Backlog*
  and *Open questions*, add every open `product` issue its opener left out of
  the `GameStack Product` Project, then confirm or change each one's Horizon:
  Now for the new cycle's product issues, Next or Later for the rest. The owner
  and number, the Project's items against the open product issues, and the add:
  ```bash
  o=$(gh repo view --json owner --jq .owner.login)
  n=$(gh project list --owner "$o" --format json --jq '.projects[] | select(.title == "GameStack Product") | .number')
  gh project item-list "$n" --owner "$o" --limit 500 --format json
  gh issue list --label product --limit 500
  gh project item-add "$n" --owner "$o" --url <issue>
  ```
  These need the token's `project` scope, granted only through a browser: if
  they fail, ask the owner to run `! gh auth refresh -h github.com -s project`.
- **Recorded** once the owner decides: rewrite the `Triage log` issue's body as
  ADR 0038 shapes it, never appending; then write the new `CYCLE.md`.

## One task per session
A task is one branch, and its session runs it to the merge or the abandonment:
evaluation, review rounds and their findings' triage, pull request, merge. The
session ends there — checkpoint and stop, even when told to continue. Never
start a second branch: a handoff that chained two tasks led an agent to start
the second in the same session, filling its context with unrelated work.

## When to checkpoint
When a `CYCLE.md` status changes (a merge, a branch abandoned, an insertion,
the cycle closing) and at session end. Not per commit: `git log` and the hook
already show those.

## Steps
1. Run the hook by hand from the repository root, for the live facts and the
   bound warnings: `bash .claude/hooks/session-start.sh </dev/null`.
2. Update `CYCLE.md`'s statuses; close the cycle if its exit holds, or stop it
   if a breaker fired. Either way a triage is now owed (§ Triage).
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
