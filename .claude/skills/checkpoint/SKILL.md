---
name: checkpoint
description: Rewrite the session handoff (HANDOFF.md) and the open cycle (CYCLE.md), and hold the cycle's rules — admission, breakers, when a triage is owed and what it proposes, one task per session. Use after a merge or an abandoned branch, when a cycle item's status changes, before admitting work to the cycle, when a triage is owed, and at session end.
---

## Why this exists
The SessionStart hook (`.claude/hooks/session-start.sh`) starts every session
from two small files and the repository facts it derives live. The files stay
small and true only if every checkpoint follows these rules, kept here rather
than in the files, which every session would pay for (ADR 0045).

## Where the files live
Outside the repository, since they are one person's live state and every pull
request diff would carry them: at the path the hook prints, in its
`== HANDOFF.md (<path>) ==` header or, with no handoff yet, its
`No handoff at <path>.` line: `$CLAUDE_HANDOFF_DIR`, or `~/Documents/<main
checkout's folder name>` when it is unset, shared by every worktree. The cycles
are kept on one machine, the one with a `CYCLE.md` or an `archive/cycles/`
beside the handoff; a shared home for a team's cycle is #159's question.

## HANDOFF.md — session state only
- **Sections:** an `Updated:` line (the date and the event that triggered the
  checkpoint), then *Now*, *In flight*, *Owed by the owner*, and no other; the
  hook warns about any other `##` heading. *Now* ends with one
  `**Next action:**` line naming exactly one task, never a "then".
- ***In flight* holds only the state of open work:** each line a pointer, an
  issue or a plan, and where it stands. Never a rule, a lesson, a method or a
  decision: no clone reads the handoff, so § Steps 5 routes them, and a section
  for what has no other home was how they got around it (ADR 0065).
- **Never a *Next* list.** What comes next is `CYCLE.md`; anything not chosen is
  a GitHub issue (ADR 0038).
- **Rewritten whole at each checkpoint, never appended.** A done item leaves:
  its record is the commit, the ADR or the issue.
- **Bound: 40 lines and 3,000 bytes.** The hook warns when it is over. Past
  10,000 characters, Claude Code replaces the hook's whole output with a file
  path and a 2,000-character preview, and does not ask the agent to read the file.
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
- **A status is a state, not a history:** `todo`, `in progress`, `done
  <date>, PR #n` (the PRs, if several), `back to the pool`. Where open work
  stands is the handoff's *In flight*; what was decided is in the issue or the
  ADR. Every session pays for the file.
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
  of three — goes back to the pool at the next triage. Before proposing an
  insertion, say whether it would be the third, and offer the open item's plan
  instead when the work fits there.
- **Closed whole** when the exit condition holds, or **stopped** when a breaker
  fires: either way, move the file to `archive/cycles/YYYY-MM-cycle-N.md`, the
  month it closed and its number, and a triage chooses the next cycle from the
  issue pool. Nothing unchosen carries over. Closing is that move: the hook
  reads a `CYCLE.md` that is present as an open cycle.

## Triage
- **Owed** when no cycle is open and when a breaker fires (ADR 0047), never by a
  merge alone, and only where the cycles are kept: elsewhere the hook says no
  cycle is kept there, and nothing blocks a branch. The hook prints `TRIAGE
  OWED` with its reason, first, before any warning, and lists the open
  issues that carry a bucket but no priority, neither `gated` nor `product`. An
  owed triage runs before the next branch starts.
- **What it proposes** to the owner, who decides: what ADR 0038 § Decision lists
  for a triage, for every open issue but the `Triage log` and the `product`
  ones. Each ungated issue's priority, proposed when it opened, is confirmed or
  changed. Then the next cycle (§ CYCLE.md), its items admitted by the clauses.
  Show the owner the pool grouped by bucket and priority, its board (ADR 0048):
  ```bash
  gh issue list --state open --limit 500 --json number,title,labels --jq '
    map({n: .number, t: .title, l: [.labels[].name]} | select(.t != "Triage log" and (.l | index("product") | not))
      | .b = (first(.l[] | select(test("^(blocks-product|defect|improvement|idea)$"))) // "no-bucket")
      | .p = (if .l | index("gated") then "gated" else first(.l[] | select(test("^p[123]$"))) // "none" end))
    | sort_by(.b, {p1: 1, p2: 2, p3: 3, none: 4, gated: 5}[.p], .n) | .[] | "\(.b) \(.p) #\(.n) \(.t)"'
  ```
- **Product issues** (ADR 0043), app bugs among them (ADR 0048): read
  `PRODUCT.md`'s *First release*, *Backlog* and *Open questions*, add every open
  `product` issue the `GameStack Product` Project lacks (its auto-add workflow
  adds an older one only once it is next updated), then confirm or change each
  one's Horizon: Now for the new cycle's product issues, Next or Later for the
  rest. The commands find the Project's owner and number, list its items and the
  open product issues to compare, and add one:
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
session ends there: checkpoint and stop, even when told to continue. Never
start a second branch, which fills the context with unrelated work.

## When to checkpoint
When a `CYCLE.md` status changes (a merge, a branch abandoned, an insertion,
the cycle closing) and at session end. Not per commit: `git log` and the hook
already show those.

## Steps
1. Run the hook by hand from the repository root, for the live facts and the
   bound warnings: `bash .claude/hooks/session-start.sh </dev/null`.
2. Update `CYCLE.md`'s statuses; close the cycle if its exit holds, or stop it
   if a breaker fired. Either way a triage is now owed (§ Triage). For an item
   marked done, and for each plan the hook says nothing cites, check that every
   decision the plan holds and every measure an open item needs is in an ADR,
   an issue or TOOLING.md. Then give the owner the command that deletes the
   plan, and record it under *Owed by the owner* until it runs: auto mode's
   classifier refuses the agent's `rm` there. Never delete the item's row
   (§ What is kept).
3. Read `HANDOFF.md` with the Read tool, then rewrite it whole with Write.
4. Check the bounds on the handoff at the path the hook's
   `== HANDOFF.md (<path>) ==` header prints: `wc -l` and `wc -c` within 40
   and 3,000, and no hash or next ADR number anywhere:
   `grep -E '\b[0-9a-f]{7,40}\b|next (free )?ADR' "<path>"`.
5. Route what the session learned, never into the handoff: what is personal to
   the owner or about this machine to agent memory, a rule change proposed to
   the owner, a decision to an ADR, an incident to the commit body, pending work
   to an issue. What would serve a clone or another person never goes to memory
   (ADR 0066).
6. At session end, stop. Report in three lines or fewer: what changed, the next
   action, anything owed by the owner.

## What is kept
Beside the two files, in the same folder, only what a later session reads
(ADR 0065):
- **Plans** in `plans/`, cited from the cycle or the handoff by their `plans/`
  path while their item is open. The owner deletes a plan once every decision
  it holds and every measure an open item needs is in an ADR, an issue or
  TOOLING.md (§ Steps 2); there is no plan archive. The hook warns about the
  plans neither file cites: the first ten by name, and a count of the rest.
- **A branch's records** in a folder named for the branch: `reviews/<branch>/`
  for review rounds, `captures/<branch>/` for device captures and a pull
  request's screenshots. The hook deletes the folder once the branch is neither
  local nor on a remote, names it, and warns about any record outside such a
  folder (ADR 0067). `reviews-tally.csv` is frozen: no row is added,
  and it goes with #100's README, which is its last reader.
- **Closed cycles** in `archive/cycles/`, the only record of what each cycle
  chose and why it ended, and the marker the hook reads for the machine that
  keeps the cycles. Kept until the cycle has a shared home, #159.
