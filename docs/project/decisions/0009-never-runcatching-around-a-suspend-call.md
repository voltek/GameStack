# ADR 0009 — Never build a `Result` with `runCatching` around a suspend call

Status: Accepted
Date: 2026-08-22
Backfilled: yes — DRIFT-LOG.md, row 2026-08-22 ("The same rationale for `try`/`catch`")
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Architecture

## Context
`runCatching` catches `Throwable`, and that includes `CancellationException`. A
coroutine that was merely cancelled — a superseded debounce, a ViewModel being
cleared — therefore comes back as `Result.failure` and reaches the user as a
real error. This was not a theoretical concern: every cancelled Search flashed
the error screen before the next results landed.

The rule had no home. Its full rationale had been written out twice, in two
different RepositoryImpls, and the navigation-structure rationale was duplicated
the same way between CLAUDE.md and code comments — exactly the duplication
CLAUDE.md's Tier 3 forbids, and a guarantee that the copies would drift apart.

## Decision
Never build a `Result` with `runCatching` around a suspend call. Use
`try`/`catch` that rethrows `CancellationException` before catching `Exception`.
This is a correctness rule, not a style preference.

And the placement rule the duplication forced, stated generally: a rule that
applies to future code lives in CLAUDE.md plus its owning Skill, and gets a
one-line pointer in the code. Rationale for one non-obvious line stays in the
code. **Never both.**

## Consequences
Cancellation stops being reported as failure, which removes a whole class of
phantom error states from any screen with a debounce or a superseded request.

The placement rule gives every future rationale exactly one home, so there is
one copy to update. The cost is that a reader at the call site sees a pointer
rather than the argument, and has to follow it.

Both halves are checkable by reading: the checklist carries a detector for
`runCatching` in a suspend function, and another for comments that duplicate
text already living in a Skill, the drift log or an ADR.
