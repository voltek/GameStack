# Decisions (ADRs) — GameStack

One file per architectural decision that is in force: what was decided, what
forced it, what it costs, and **who actually enforces it**.

## What belongs here
A decision that is *born or changes*. That is the whole boundary, and it is the
other half of `../DRIFT-LOG.md`, which records incidents — every time the code
and a document disagreed, and which of the two was changed. An incident is past
tense and belongs in the log; a decision is present tense and belongs here. One
drift often produces both, and then the log row points at the ADR instead of
restating it: two copies of the same reasoning drift apart, which is why
CLAUDE.md already forbids writing it in both places.

A Skill's procedure parameter is not a decision; where the line falls, and how
to test a change against it, is
[ADR 0032](0032-a-skill-parameter-is-not-a-decision.md).

`../DRIFT-CHECKLIST.md` is the third file and the imperative one — the violation
detectors you run to *find* drift.

## Naming and numbering
- `NNNN-slug-in-kebab-case.md`, four digits, zero-padded.
- **Numbered in the order decisions are recorded** — always the next free
  number. Chronology is read from `Date:`, and `Backfilled:` explains any gap
  between when a decision was taken and when its file was written; neither is
  ever inferred from the number
  ([ADR 0023](0023-adr-numbers-follow-recording-order.md)).
- `0001`–`0019` were numbered by date during the backfill, so for those the two
  orders happen to coincide. That is a fact about how they were written, not a
  promise the numbering makes. There is no tie-break rule, because recording
  order admits no ties.
- **A file is never renamed** — not even once its decision is superseded. That
  is what makes the number a stable address.

## The template

```markdown
# ADR 0014 — The force-push window closes at the merge

Status: Accepted
Date: 2026-08-23
Backfilled: yes — DRIFT-LOG.md, row 2026-08-23 ("Two workflow rules were producing the mess")
Supersedes: ADR-0007
Enforced by: branch protection — `allow_force_pushes=false` (no force-push to `main`)
Enforced by: none (prose only — CLAUDE.md § Git workflow Tier 2) — the `--force-with-lease` window, bare `--force`

## Context
What situation existed and what forced it. 3-6 sentences, in the past tense.

## Decision
What was decided, in the present tense and imperative. The rule, not the story.

## Consequences
What it buys and what it costs, including what was accepted knowingly. In the
past tense.
```

## The fields
- **`Status:`** — `Accepted`, or `Superseded by ADR-00NN`. **Live** — see
  *Immutability*. It may be scoped when only part of the file was replaced:
  `Superseded by ADR-00NN — <the clause>`. An unscoped `Accepted` line then
  covers whatever no scoped line names, and `grep "^Status: Superseded"` finds
  every ADR that is superseded in whole or in part. Superseding means replacing
  or contradicting a clause: a later ADR that only adds to a rule — a label on
  the issue the rule already opens — leaves its `Status:` as it is.
- **`Date:`** — the date of the *original* decision: the day it was approved,
  even when the file is written later. `Backfilled:` is what says that it was.
- **`Backfilled:`** — `yes — DRIFT-LOG.md, row <date> ("<opening words>")`, or
  `no`. Where one decision was backfilled from more than one row, the plural
  form `rows <date> ("<first>") and ("<second>")` addresses them all. The quoted
  opening words are what make an address resolve: 26 rows share 2026-08-23, so a
  bare date points at all of them. And without the field at all, a file written
  today looks like a decision taken today, which falsifies the record.
- **`Supersedes:` / `Superseded by:`** — only when one applies. `Supersedes:` is
  written when the superseding ADR is, so it is frozen, and may carry a scope
  the same way `Status:` does. Where the two sides disagree, read the
  superseded ADR's `Status:`: it is the live one.
- **`Enforced by:`** — one line per clause, each a value from the closed
  vocabulary below. **Live** — see *Immutability*.

### `Enforced by:` — one line per clause
An ADR whose rule is policed, or unpoliced, as a whole carries one line with no
scope. An ADR whose clauses differ carries one line per group of clauses, and
**every line then says what it covers**, after ` — `:

```
Enforced by: repository settings — `allow_rebase_merge=false` (the Rebase and merge button)
Enforced by: none (prose only — CLAUDE.md § Git workflow Tier 2) — `git merge main` instead of `rebase`, the 400-line alarm
```

Some values contain ` — ` themselves, so match a line against the vocabulary as
a literal prefix first; the scope is whatever follows the complete value. A line
naming `branch protection` or `repository settings` names the setting it relies
on in backticks as `field=value`, or a bare `field` when the rule is simply
present, using the name GitHub's API uses, because that is what
`../DRIFT-CHECKLIST.md` compares with the repository.

### `Enforced by:` — closed vocabulary
- `none (prose only — <document> § <section>)` — CLAUDE.md in almost every case;
  name the document that actually carries the rule, since a few live in the Spec
  rather than in CLAUDE.md (product scope, not a constraint on code), and a few in
  a Skill (a procedure attached to a named task, which CLAUDE.md's three
  questions send there rather than into CLAUDE.md itself).
- `checklist detector — DRIFT-CHECKLIST.md § <section>`
- `./gradlew lint`
- `test: <TestName>#<caseName>`
- `gate.yml`
- `branch protection` — a rule on the `main` branch, read with
  `gh api repos/{owner}/{repo}/branches/main/protection`. It binds
  administrators only while `enforce_admins` is on, so a rule an administrator
  can step around is policed only against everyone else, and its line says so,
  naming `enforce_admins=false` for as long as that is the case.
- `repository settings` — a setting of the repository itself, such as which merge
  buttons exist, read with `gh api repos/{owner}/{repo}`. Not the same thing as
  `branch protection`, and a reader needs to know which of the two to look at.
- `claude settings` — a `deny` or `ask` rule in the committed
  `.claude/settings.json`, named as `deny` or `ask` plus what it matches in words;
  the file holds the exact patterns. It binds an agent session, never a person,
  so the line says `(the agent only, common spellings)`. A pattern misses other spellings
  of the same command: it stops a mistake, not an intent.

This field is as much the point of the folder as the decision text is: **an ADR
with no fitness function behind it is a wish.** This repository has already
proved that — rules written in CLAUDE.md and still violated two and three times
over. So `none` is the honest answer far more often than not, and it is a
finding rather than a gap to paper over. Read the whole map with:

```bash
grep -H "^Enforced by:" docs/project/decisions/[0-9]*.md
```

and the wishes with `grep -H "^Enforced by: none" docs/project/decisions/[0-9]*.md`.
An ADR with one unwatched clause among several still appears in that second
list: a partly policed decision is partly a wish, and it leaves the list only
when its last unwatched clause is covered.

## Immutability
An ADR records what was decided *then*. It is not edited to reflect a later
change of mind, and it is not corrected because the decision reads badly today —
if it is genuinely wrong, write a **new** ADR that supersedes it. The two files
side by side are the record of the mind changing, which is the part worth
keeping.

**Two lines are live and the rest is frozen: `Status:` and `Enforced by:`.**
They are the two facts about an ADR that change without its decision changing —
whether it still stands, and what polices it today — so they are the only lines
an accepted ADR ever takes edits to. Everything else stays as written. A fully
superseded ADR's `Enforced by:` is left as it was when it was superseded.

Because frozen text cannot be corrected, it **holds no fact that can change**.
Context and Consequences describe what was true when they were written, in the
past tense, and so cannot go stale; a setting's value, a count or the state of a
file belongs in a live field or in `../DRIFT-LOG.md`. Decision is the exception
to the tense rule, not to the principle: it states a rule in the imperative, and
a rule is not a fact about the world
([ADR 0024](0024-enforced-by-and-status-are-live-and-per-clause.md)).

## Dates
Every date in this repository — `Date:`, the row a `Backfilled:` line addresses,
a `../DRIFT-LOG.md` row — is the **local calendar date**, never UTC. After
roughly 18:00 local time, UTC is already tomorrow, and a date a day ahead cannot
be renamed away afterwards: `Backfilled:` addresses log rows by date, so a wrong
one points at a row that does not exist.

## No index
There is deliberately no hand-written list of ADRs here. It would be a second
place to update per ADR, and this repository has already been burned by exactly
that: CLAUDE.md's Skills list drifted out of sync with `.claude/skills/` and
needed its own drift detector to catch it. Use the `grep` above.
