# ADR 0024 — `Enforced by:` and `Status:` are live and take one line per clause

Status: Accepted
Status: Superseded by ADR-0033 — "or in `DRIFT-LOG.md`" as a home for a fact that can change (Decision, fifth paragraph)
Date: 2026-09-01
Backfilled: yes — DRIFT-LOG.md, row 2026-09-01 ("ADR 0008 bans `Rebase and merge`, and the button was still enabled")
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Documentation integrity — a `branch protection` or `repository settings` line the repository no longer backs
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Documentation integrity — the field rules, multi-line `Enforced by:` and scoped `Status:` included
Enforced by: none (prose only — decisions/README.md § Immutability) — the two live fields, and the past-tense convention for frozen sections
Enforced by: none (prose only — decisions/README.md § Dates) — local dates, never UTC

## Context
`Enforced by:` took exactly one value and was frozen with the rest of the file.
Both properties failed on 2026-09-01, when the `Rebase and merge` button was
switched off. ADR 0008 then understated what was true, and nothing in the system
could correct it: the immutability rule allowed only `Status:` to change, and one
value could not say that the button was policed while the branch-update rule and
the PR-size alarm were not. A fix that made the field live was drafted, then
withdrawn after review: it obliged a sweep of every ADR and a detector for the
GitHub state it would assert, and neither could be done honestly with one value.
ADR 0010 (lint covered one clause of two) and ADR 0008 (a setting covered one
rule of three) had already hit the same limit, and the withdrawn fix introduced
the next defect in the same place, so the rule of three applied.

The same shape appeared on `Status:` on 2026-09-02. ADR 0023 replaced one rule of
ADR 0020's Decision, which 0020 states twice. `Status:` applied to the whole
file, and superseding 0020 outright would have marked the entire ADR system as
retired, so 0020 stayed `Accepted` with a withdrawn rule and nothing pointing at
its replacement. Facts about the world had a third home in the same trap: ADR
0008 said the repository "still has" the button enabled, a sentence frozen into a
file that cannot be corrected.

## Decision
`Enforced by:` takes one line per clause. Each line is a value from the closed
vocabulary, then ` — ` and what it covers. An ADR whose rule is policed or
unpoliced as a whole keeps a single line with no scope. A line naming
`branch protection` or `repository settings` names the setting as `field=value`,
or a bare `field` when the rule is simply present, so it can be compared with
the repository.

`grep "^Enforced by: none"` therefore keeps listing an ADR for as long as any
clause of it is unwatched. A partly policed ADR is still partly a wish, and it
leaves that list when its last unwatched clause is covered.

`Status:` takes the same form. A line may be scoped — `Superseded by ADR-00NN —
<the clause>` — and an unscoped `Accepted` line covers whatever no scoped line
names. `Supersedes:` may carry a scope the same way. When the two sides
disagree, the superseded ADR's `Status:` is the one to read, because it is live.

`Enforced by:` and `Status:` are the only lines of an accepted ADR that may be
edited; everything else stays frozen. A fully superseded ADR's `Enforced by:` is
left as it was when it was superseded.

Frozen sections hold no fact that can change. Context and Consequences say what
was true when they were written, in the past tense; a setting's value, a count or
the state of a file goes in a live field or in `DRIFT-LOG.md`. Decision stays
what it always was, the rule in the imperative.

Dates in this repository are local calendar dates, never UTC.

## Consequences
The map was made true clause by clause. The sweep that came with this change
rewrote `Enforced by:` in ADRs 0006, 0008, 0010, 0014 and 0015, and scoped the
`Status:` of ADR 0020.

The sweep read all twenty-three earlier ADRs against the settings. The other
eighteen kept their lines: no setting polices a clause of them, and ADR 0007 is
superseded in whole. A checkbox in the pull request template prompts a person
and fails nothing, so it was not counted as a mechanism.

The sweep's `branch protection` lines record the administrator gap in their own
scope: the protection on `main` did not bind administrators at the time, and the
maintainer is one, so every one of those rules was prose against them. "Never
commit directly to `main`" is the clause that gained its protection line only
with that qualification. That is a finding about the repository, left for the
maintainer to act on rather than decided here.

The costs, accepted knowingly. Two fields became editable after the fact, so how
coverage looked at an earlier date survives only in `git log`. The scope after
` — ` is prose that a script has to parse, and some values contain ` — ` of their
own, so a reader or checker matches the vocabulary as a literal prefix first. A
line that asserts GitHub state is only as true as the last time something
compared it with GitHub, which is why the detector exists and why it is run by a
person for now. And the past-tense rule is a convention with no check behind it.
