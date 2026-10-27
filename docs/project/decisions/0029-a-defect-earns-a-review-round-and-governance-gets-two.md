# ADR 0029 — A defect earns a review round, and governance text gets two

Status: Accepted
Status: Superseded by ADR-0030 — wording, duplication and friction without a failure scenario are cheap or deferred
Date: 2026-10-23
Backfilled: no
Supersedes: ADR-0025 — the loop ends on an event, never a number of rounds; a round whose defects come mostly from the previous round's fixes ends it
Enforced by: none (prose only — .claude/skills/ship-a-branch/SKILL.md § Triage every finding into one bucket, and say which) — the two kinds of fix, the defect's origin, reverting a cheap fix, the cap, the single redesign, the fix no round reads

## Context
ADR 0025 let a round earn another when its confirmed table fixed a real defect,
ended the loop on a round fed by the previous round's fixes, and allowed no
count of rounds. `ship-a-branch`, not the ADR, defined a governing document's
real defect as "a contradiction, a broken reference, or a rule an agent cannot
follow as written", and its fix bucket also held anything cheap enough to fix.
Across PRs #36 to #43, seventeen rounds on five governance or workflow branches,
no loop ended on the primary signal: each ended on the rule of three or on the
owner. A reviewer of prose found a defect every round, and every fix wrote new
prose for the next round. Nothing bounded redesigns, and the GitHub bot's quota
was too small to serve as the backstop.

## Decision
A fix is `Fix (defect)` or `Fix (cheap)`, and the triage table says which. In
code a defect is correctness, data loss or something the user can perceive. In a governing document it needs a
failure scenario in which an agent following the text acts wrongly; wording,
duplication and friction without one are cheap or deferred. Only a round with a
`Fix (defect)` row can earn another.

Every defect names its origin: the diff the first round read, a cheap fix or a
defect fix of an earlier round, or something else, which counts as the diff. Revert a cheap fix that introduced a defect; a
revert earns no round. A round whose remaining defects come mostly from defect
fixes goes back to the human.

Round three or later is earned only by a defect in a file outside the governance
shape (`ship-a-branch` § A governance diff earns the checklist). A branch gets one
redesign, which restarts the count; if the rule of three fires on it again, that
part leaves the branch for a plan. A fix no round will read reverts where it
can, is otherwise minimal, re-runs the gate and the detectors, and is listed in
the PR body. The human may always ask for another round.

Keep the procedure in `ship-a-branch`.

## Consequences
The table became the stopping rule's input, so the verdict could be read off it
rather than rebuilt from memory. Governance text got a ceiling of four rounds,
two before its redesign and two after. Code kept no count: a defect fix there
carried a regression test, and no code branch had been measured under ADR 0025.

Accepted knowingly: a governing document's last fixes shipped read only by the
detectors, and by the GitHub bot when its quota allowed. No known defect shipped
unfixed unless the human deferred it.

Nothing mechanical enforced it.
