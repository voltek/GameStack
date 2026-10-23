# ADR 0025 — A review round closes on a confirmed triage

Status: Accepted
Status: Superseded by ADR-0029 — the loop ends on an event, never a number of rounds; a round whose defects come mostly from the previous round's fixes ends it
Date: 2026-10-13
Backfilled: no
Supersedes: ADR-0015 — every finding fixed or dismissed with a written reason; a round producing a real defect earns another round
Enforced by: none (prose only — CLAUDE.md § Git workflow Tier 2) — the human confirms the triage
Enforced by: none (prose only — .claude/skills/ship-a-branch/SKILL.md § Triage every finding into one bucket, and say which) — the three buckets, the `review-debt` issue, the recurring false positive
Enforced by: none (prose only — .github/pull_request_template.md § Verification) — the `Deferred:` and `Dismissed:` lines
Enforced by: none (prose only — .claude/skills/ship-a-branch/SKILL.md § 3. Reviews — all of them, before the PR exists) — `/code-review` on every branch but step 3's two exemptions

## Context
ADR 0015 required every finding to be fixed or dismissed with a written reason,
but not where the reason went. With the GitHub bot the thread was the record; a
local `/code-review` has no thread, so on PR #33 the dismissals lived only in the
conversation, in a file kept outside the repository, and in the PR body. That
round's triage was presented after the commit had already been amended. On
PR #32, two of the second round's defects had been introduced by the fixes to
the first. And `ship-a-branch` exempted a docs-only diff from `/code-review`,
although the round PR #33 ran anyway changed what that branch shipped
(`1dcd5d1` among it).

## Decision
Triage every finding into one of three buckets — fix (anything fixed on the
branch), defer, dismiss — and propose the whole table before touching anything.
The human confirms the table; choosing a bucket is plan-level, because a defect
filed as debt leaves the branch exactly as a dismissed one does.

A deferred finding gets an issue labelled `review-debt`, always. A dismissed
finding gets its verified reason in its thread. A local `/code-review` has no
thread, so its record is saved outside the conversation as soon as the table is
confirmed, and reaches the PR body's `Deferred:` and `Dismissed:` lines. A false
positive that recurs is a gap in the document: fix the text, and keep no ledger
beyond those records.

A round earns another only when its confirmed table fixes a real defect on the
branch; a defect the human defers earns none. Apply the rule of three to rounds:
a round whose defects come mostly from the previous round's fixes ends the loop
and goes back to the human. The trigger is
that event, never a number of rounds. Its findings are still triaged, and the
branch waits for the human's decision.

Keep the procedure in `ship-a-branch` alone. CLAUDE.md carries only the rule that
the triage is confirmed, and the template only names the record.

Run `/code-review` on every branch but a comments-only diff and a docs-only diff
outside the shape ADR 0022 defines. A governance diff runs it alongside the
*Documentation integrity* detectors, not instead of them.

## Consequences
Two clauses of ADR 0015 were superseded rather than added to. Its two outcomes,
fixed or dismissed, became three once a valid finding could leave as debt, and
its round with a real defect always earned another, where a round fed by its own
fixes now ended the loop. Its stopping signal stood, widened to any round that
fixed no real defect, and so did its rule of three.

The single copy was earned on this decision's own branch: written into four
documents, each review fix to one copy put another out of step, until a round
was mostly those defects.

The cost was accepted knowingly. The reviewer read the repository and not the
records of earlier rounds, so no register prevented a dismissed finding from
returning; only a document that answered it could. And a governance, workflow or
build PR cost the human one more `/code-review`.

Nothing mechanical enforced any of it; a label and two template lines were the
whole of the artifacts it left.
