# ADR 0033 — Drift is recorded as a commit trailer; the drift log is a frozen archive

Status: Accepted
Date: 2026-10-26
Backfilled: no
Supersedes: ADR-0018 — "and the drift log, both of which this project maintains" (Decision, question 2)
Supersedes: ADR-0020 — the `DRIFT-LOG.md` bullet, and "an incident is a log row, never an ADR. Where one drift produces both, the row points at the ADR instead of restating it" (Decision)
Supersedes: ADR-0024 — "or in `DRIFT-LOG.md`" as a home for a fact that can change (Decision, fifth paragraph)
Supersedes: ADR-0028 — "and log a DRIFT-LOG row" (Decision, second paragraph); the rest of that paragraph stands
Enforced by: git hook — .githooks/commit-msg — the three values of a `Drift:` trailer, the form of a `Bypassed:` trailer
Enforced by: commits.yml — the same two forms on every commit of a PR
Enforced by: none (prose only — CLAUDE.md § Self-Healing Loop) — which commits carry a `Drift:` trailer
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Documentation integrity — the frozen log (any change to it), the commit and PR forms of `Backfilled:`
Enforced by: none (prose only — decisions/0033-drift-is-a-commit-trailer.md § Decision) — which commits carry a `Bypassed:` trailer, where history leaving a document goes

## Context
`DRIFT-LOG.md` had reached 70 rows and 67,885 bytes, about 950 characters a
row, and grew with every drift. Read row by row, about half were drift; the rest
were code defects that contradicted no document, or decisions and process
narrative, 21 of which already had an ADR backfilled from them. Every row's
diagnosis was also in the body of the commit that resolved it, so the log was a
second copy, which CLAUDE.md's comment rule names as the copy nobody updates.
Its header promised a use, spotting a pattern repeat, that no document, Skill or
checklist step performed. Its only machine reader was the checklist detector
that resolves the `Backfilled:` addresses.

## Decision
A commit that resolves a drift carries the trailer
`Drift: code fixed`, `Drift: doc updated` or `Drift: both`, and no other value.
Drift is code, an asset or a setting disagreeing with a governing document, or
two governing documents prescribing different actions. A code defect that no
document contradicted is not drift: it gets its fix and a regression test. A
rule that is born or changes is still a new ADR, named in the commit body.

`DRIFT-LOG.md` is a frozen archive: it takes no new rows, and its rows are never
edited. It stays at its path, because `Backfilled:` lines address it. A decision
recorded late from now on points at where it was taken:
`Backfilled: yes — commit <sha>` or `Backfilled: yes — PR #<n>`.

History leaving a governing document goes to the body of the
commit that removes it. Never to the archive.

Each use of ADR 0028's emergency exit is recorded on the commit that lands on
`main` through it, as the trailer `Bypassed: enforce_admins — <reason>`.

Read both with `git log --grep='^Drift:'` and `git log --grep='^Bypassed:'`.

## Consequences
The record of which way a drift was resolved stayed, in the change's own
metadata, in every clone and through both merge strategies, since a squash keeps
the message text. The second copy of each diagnosis stopped growing. The
trailer's vocabulary could be checked by a `commit-msg` hook once one existed
(TOOLING-BACKLOG #5); whether a commit was drift stayed a judgement. Lost,
knowingly: one readable page of incidents going forward, and a merge through
GitHub's button needed the owner to type the `Bypassed:` trailer into the merge
commit's description.
