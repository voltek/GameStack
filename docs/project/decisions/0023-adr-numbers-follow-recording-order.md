# ADR 0023 — ADR numbers follow the order decisions are recorded

Status: Accepted
Date: 2026-09-02
Backfilled: no
Enforced by: none (prose only — decisions/README.md § Naming and numbering)

## Context
The rule that ADRs are numbered by the date of the original decision was written
while backfilling nineteen historical decisions, where every date was known and
fixed before a single file existed. It has come up short twice since. On
2026-09-01 the tie-break for decisions sharing a date turned out not to
determine ADR 0019's position at all, and was amended. Today the ordering itself
failed in the forward direction: ADR 0021 was decided and written on 2026-09-02,
while ADR 0022 was approved on 2026-09-01 and written the day after, so the
older decision took the higher number — permanently, because files are never
renamed. The same collision is already queued for the `Enforced by:` redesign,
approved on 2026-09-01 and due to be written after 0022 lands, for which no
number below 0022 exists. Meanwhile nothing in this repository sorts, groups or
reads ADRs by number, and the README's own justification for the number is that
it is a stable address.

## Decision
ADR numbers are assigned in the order decisions are recorded — always the next
free number. Chronology is read from `Date:`, which carries the date of the
original decision, and from `Backfilled:`, which explains any gap between that
date and the day the file was written. Neither is inferred from the number.

`0001`–`0019` keep the numbers the backfill gave them, so for those the two
orders coincide; that is a fact about how they were written, not a promise the
numbering makes. Files are still never renamed. The tie-break rule for decisions
sharing a date is withdrawn, because recording order admits no ties.

## Consequences
The number goes back to being only what it was ever justified as: a stable
address. The property it loses was never load-bearing — no tool, grep or
document ordered by it — and it had already failed in practice: of the three
ADRs written since the backfill, ADR 0022 carries an earlier date than ADR 0021
while holding the higher number.

What it costs a reader: where a decision sits in the folder no longer tells you
when it was taken, so `Date:` has to be read. That is one field, and it is the
field that was always authoritative.

The cost that is not clean, stated plainly: this withdraws a rule ADR 0020
states in two places — its Decision, and again in its closing Consequences
paragraph — while the four other rules that ADR carries are untouched and in
force. `Status:` applies to a whole file, so the corpus cannot record that part
of an ADR has been replaced while the rest stands, and superseding 0020 outright
would mark the entire ADR system as retired, which is false. So 0020 keeps
`Status: Accepted`, and a reader arriving at either of those two places finds
the withdrawn rule with nothing pointing here. That is the same per-clause gap
the `Enforced by:` redesign is being built to close for a different field, and
the fix should be extended to `Status:` when it lands.
