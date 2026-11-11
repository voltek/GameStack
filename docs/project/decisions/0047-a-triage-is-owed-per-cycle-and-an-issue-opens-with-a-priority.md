# ADR 0047 — A triage is owed per cycle, and an issue opens with a priority

Status: Accepted
Status: Superseded by ADR-0048 — "A harness issue, an app defect among them, opens with one bucket" (Decision)
Status: Superseded by ADR-0065 — "first after any bound warning" (Decision)
Date: 2026-11-01
Backfilled: no
Supersedes: ADR-0038 — "once a merge changes `app/src/main`, or three merges have landed since the last triage" (Decision)
Supersedes: ADR-0038 — "the owner confirms the bucket and sets one priority within it" (Decision)
Supersedes: ADR-0038 — "Labels when opened" (Decision)
Enforced by: test: test-session-start.sh#no open cycle — a triage owed when no cycle is open
Enforced by: test: test-session-start.sh#no cycle kept on this machine — nothing owed where no cycles are kept
Enforced by: test: test-session-start.sh#a third insertion — a triage owed when a breaker fires
Enforced by: test: test-session-start.sh#an ungated issue with no priority — the hook lists them
Enforced by: none (prose only — .claude/skills/checkpoint/SKILL.md § Triage) — a stopped cycle archived, the triage choosing the next one
Enforced by: none (prose only — docs/project/TOOLING.md § 12. Issue form) — the labels an issue opens with, a priority or a Horizon proposed, confirmed or changed at triage
Enforced by: none (prose only — .claude/skills/checkpoint/SKILL.md § CYCLE.md — one finite cycle) — Now held by the open cycle's product issues

## Context
ADR 0038 owed a triage once a merge changed `app/src/main` or three merges had
landed, a rule written before cycles existed. Cycles then decided what was
worked next, and a severe defect entered one as an insertion with no triage, so
a triage in the middle of a cycle had little left to choose. The count fired on
merges of any size: a one-line `.gitignore` pull request made it fire in the
middle of cycle 3, and in a product cycle nearly every pull request changed
`app/src/main`. Issues opened with a bucket and no priority, so eleven waited
unprioritised at the triage of 2026-11-01; a product issue's proposed Horizon
reached the Project only at a triage, and Detail's issue held Now while cycle 4
was harness work. The SessionStart hook read the `Triage log` only to anchor
the merge count.

## Decision
- **A triage is owed when no cycle is open**, the last one closed or stopped,
  **and when a breaker fires in the open one**, as the `checkpoint` Skill names
  them. A merge owes nothing by itself. A breaker stops its cycle, which is
  archived like a closed one, and the triage chooses the next. It is owed to
  whoever keeps the cycles: on another machine, with no `CYCLE.md` and no
  archive of them, no cycle is kept and nothing is owed there. A machine starts
  keeping cycles with its first triage, which no hook can owe.
- **Every issue opens with its priority proposed, in the field its tracker
  uses.** A harness issue, an app defect among them, opens with one bucket, a
  `p1`–`p3` by ADR 0038's meanings unless it is `gated`, `export` and `gated`
  as ADR 0038 defines them, labels by role, several to an issue; a `product`
  issue is added to the `GameStack Product` Project with its form's Horizon. The
  triage confirms or changes either. A `review-debt` issue, and one whose author
  could not set labels, takes its bucket and priority at the triage. Wherever a
  kind of issue comes to be tracked, its opener proposes that tracker's priority.
- **Now holds the open cycle's product issues, and only those.** A product issue
  admitted to a cycle, as an item or an insertion, takes Now; at a triage, a Now
  issue the new cycle does not hold moves to Next or Later. These two add to
  ADR 0043's horizons and replace none of it.
- **The SessionStart hook reports whether a triage is owed, and why**, from
  `CYCLE.md` and the archive beside it, first after any bound warning, and lists
  every open issue that carries a bucket and no priority and is neither `gated`
  nor `product`; one with no bucket is waiting for the triage, and is counted
  as such.

## Consequences
A triage ran where a cycle needed choosing, and a product cycle no longer owed
one per pull request. The hook dropped its second `gh` call, and with it the
`Triage log` read; the log's `Last triage:` line stayed as a record only.
Accepted knowingly: each `gated` item's *Trigger* was checked once per cycle
instead of every few merges, so an event that fired mid-cycle waited for the
cycle to end unless it entered as an insertion; a proposed priority and a
confirmed one were the same label, since every triage proposes one for every
ungated issue anyway; the hook did not read the Project, which took the
`project` scope and a third `gh` call, so a product issue opened outside it
waited for the triage's pass; and the cycle stayed one machine's file, so on a
team nobody else saw it, a shared home being #134's question. Ruled out:
excluding merges that touched neither the product nor the rules, which needed a
hand-kept list of paths and left the per-pull-request triage of a product cycle
in place; a triage owed wherever `CYCLE.md` was missing, which would have
blocked every branch of a teammate who kept no cycles; and owing it wherever
`HANDOFF.md` existed, since a teammate may keep a handoff and no cycles.
