# ADR 0071 — A triage table keeps only what a later step reads

Status: Accepted
Date: 2026-11-12
Backfilled: no
Enforced by: none (prose only — .claude/skills/review-round/SKILL.md § The table) — the claim in 12 words or fewer, the state on the Owner decisions line
Enforced by: none (prose only — .claude/skills/review-round/SKILL.md § Verifying) — which buckets fill the Evidence column

## Context
Each review round wrote its triage to a file beside the handoff, one row per
finding with a claim and the evidence of its check in prose, and marked the file
proposed and then confirmed. Since ADR 0058 the chat no longer showed the table,
so the owner rarely read it (#94). Across 40 session transcripts with a triage,
the tables cost about 3,000 output tokens per branch, near 2% of what a session
wrote. The claim and the evidence made up 77% of their characters, the origin
2%. Only once was a table file opened again after being written; every other
later touch was an edit marking it confirmed. Round 2's origin check and the PR
body's *Review* were written from the conversation.

## Decision
Keep the table in its file, the record that survives a compaction or a new
session, and record in it only what a later step reads: the id, the claim in 12
words or fewer with its `file:line`, the bucket, a defect's origin, the commit
reviewed, the verdict and the human's decisions. The Evidence column holds a
code defect's regression test, a deferral's issue, a dismissal's quote or
command, an acceptance's reason, and a last-round cheap fix's test; it stays
empty for every other row.
The *Owner decisions* line holds the table's state, and the title none.

Every check still runs before the table; only its record shrinks.

## Consequences
A table cost about half as many output tokens, and the claim it held was the one
the message already used, written once for both. A governance defect's quote
lived only in the conversation, where the human judged it, so a session that
lost the conversation could not reread why that row was judged a defect; the
raw file still held the finding, and the human could always ask. The measure counted characters written, not tokens attributed to
each call, so the saving was an estimate.
