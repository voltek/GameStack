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

`../DRIFT-CHECKLIST.md` is the third file and the imperative one — the violation
detectors you run to *find* drift.

## Naming and numbering
- `NNNN-slug-in-kebab-case.md`, four digits, zero-padded.
- **Numbered by the date of the original decision**, not by when the file was
  written. `0001` is the oldest decision this repository has.
- Ties within one date break by the order of the CLAUDE.md section the rule
  belongs to, top to bottom. Decided once; not revisited.
- **A file is never renamed** — not even once its decision is superseded. That
  is what makes the number a stable address.

## The template

```markdown
# ADR 0012 — The force-push window closes at the merge

Status: Accepted
Date: 2026-08-23
Backfilled: yes — DRIFT-LOG.md, row 2026-08-23 ("Two workflow rules were")
Supersedes: ADR-0009
Enforced by: none (prose only — CLAUDE.md § Git workflow Tier 2)

## Context
What situation existed and what forced it. 3-6 sentences.

## Decision
What was decided, in the present tense and imperative. The rule, not the story.

## Consequences
What it buys and what it costs, including what was accepted knowingly.
```

## The fields
- **`Status:`** — `Accepted`, or `Superseded by ADR-00NN`.
- **`Date:`** — the date of the *original* decision.
- **`Backfilled:`** — `yes — DRIFT-LOG.md, row <date> ("<opening words>")`, or
  `no`. The quoted opening words are what make the address resolve: 26 rows
  share 2026-08-23, so a bare date points at all of them. And without the field
  at all, a file written today looks like a decision taken today, which
  falsifies the record.
- **`Supersedes:` / `Superseded by:`** — only when one applies.
- **`Enforced by:`** — exactly one value from the closed vocabulary below.

### `Enforced by:` — closed vocabulary
- `none (prose only — CLAUDE.md § <section>)`
- `checklist detector — DRIFT-CHECKLIST.md § <section>`
- `./gradlew lint`
- `test: <TestName>#<caseName>`
- `gate.yml`
- `branch protection`

This field is as much the point of the folder as the decision text is: **an ADR
with no fitness function behind it is a wish.** This repository has already
proved that — rules written in CLAUDE.md and still violated two and three times
over. So `none` is the honest answer far more often than not, and it is a
finding rather than a gap to paper over. Read the whole map with:

```bash
grep -H "^Enforced by:" docs/project/decisions/[0-9]*.md
```

## Immutability
An ADR records what was decided *then*. It is not edited to reflect a later
change of mind, and it is not corrected because the decision reads badly today —
if it is genuinely wrong, write a **new** ADR that supersedes it and set the old
one's `Status:`. The two files side by side are the record of the mind changing,
which is the part worth keeping. The only edit an accepted ADR ever takes is
that `Status:` line.

## No index
There is deliberately no hand-written list of ADRs here. It would be a second
place to update per ADR, and this repository has already been burned by exactly
that: CLAUDE.md's Skills list drifted out of sync with `.claude/skills/` and
needed its own drift detector to catch it. Use the `grep` above.
