# ADR 0063 — A PR body's review names what no round read

Status: Accepted
Date: 2026-11-10
Backfilled: no
Supersedes: ADR-0057 — "then one review record collapsed under a summary of its counts. A branch exempt from review writes its exemption in one line instead. A `TODO` left there fails the check." (Decision)
Enforced by: commits.yml — *Review* after *Verification*, its first line as the rounds run or an exemption, something after an open first line, none of the template's samples left, no triage id anywhere in the body
Enforced by: none (prose only — .claude/skills/open-pr/SKILL.md § 2. The body) — that the list holds every fix made after the last round, a deferred issue under `Related:`, a dismissal proven only outside the repository under `Evidence:`
Enforced by: none (prose only — .claude/skills/review-round/SKILL.md § Buckets) — a `review-debt` issue describing each finding rather than citing its id

## Context
The review record collapsed at the end of a PR body carried the counts and one
line per deferred, accepted and dismissed finding, each cited by its triage id
with about twenty words of reason. The ids were keys into triage tables kept
outside the repository and deleted at the merge, so after it they resolved to
nothing, and the `review-debt` issues copied them too. The words were kept so a
recurring finding could be answered once the tables were gone, yet the reviewer
never read earlier records. PR #191 carried 8 lines for 13 findings. The owner,
who acted on the record at the merge, had already confirmed every accepted and
dismissed finding in the conversation (#192).

## Decision
A PR body has a *Review* section after *Verification*. Its first line says how
many rounds ran and ends `Fixed after the last:`, followed by one list item per
fix no round read, in plain words, or by `none`. A branch exempt from review
writes `Not run — exempt: <reason>` instead.

Nothing else from the triage reaches the body. An issue a round deferred goes on
the *Issues* section's `Related:` line, and a dismissal proven only by something
seen outside the repository goes in `Evidence:`, in one sentence. Neither the
body nor a `review-debt` issue cites a finding by its triage id: each describes
what the finding was.

## Consequences
The body stopped growing with the number of findings, and what stayed was the
one part with no other copy: the fixes no reviewer read, which a fold merges
into the commits they correct. The section was left open and unbounded, because
it came after *What changed* and *Why* and a long list there was a signal for
the owner rather than a cost to the first screen.

The counts of accepted and dismissed findings left the body, and with them the
false-positive rate ADR 0058 had kept readable; no reader measured it once the
tally was frozen out of the repository. A recurring finding stayed answered where
review-round already sent it, in the text the reviewer reads. ADR 0058's Accept
lost the record line and the check its Consequences named, but not a clause of
its Decision, which never said where an accepted finding was recorded, so ADR
0058 was not superseded.

The check read the first line and the template's own samples, never the lines
after it: those were Markdown for the owner, and two review rounds of judging
them line by line had rejected valid lists and let prose through. It caught an
id in any form `r<round>-<n>`, so a body that needed such a token in prose wrote
it as inline code. Issue text stayed outside any check.
