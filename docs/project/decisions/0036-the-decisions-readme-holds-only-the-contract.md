# ADR 0036 — The decisions README holds only the contract an ADR writer needs

Status: Accepted
Date: 2026-10-27
Backfilled: no
Enforced by: none (prose only — decisions/README.md § What this README may hold)

## Context
ADR 0020 placed the format, the vocabulary and the immutability rule in
`decisions/README.md`, and the README defined what became an ADR, but nothing
defined what the README itself could hold. It had grown to 177 lines carrying
history its ADRs already held: how `0001`–`0019` were numbered, the incidents
behind "an ADR with no fitness function is a wish", and the drift behind having
no index. Each had to be trimmed by hand, and nothing stopped the next one from
entering.

## Decision
`decisions/README.md` holds only the contract an ADR writer needs: what becomes
an ADR, naming and numbering, the template, each field's format, the
`Enforced by:` vocabulary, how to read the set, immutability and dates, pointers
to the documents that take over where it stops, and at most a clause of why so a
rule does not read as arbitrary.

It does not hold why a rule was adopted (the ADR that adopted it), history or
incidents (commit bodies), or a fact that can change, such as a count or the
state of the repository (a live field or git). A line that fails this leaves,
with a pointer only when the contract would be incomplete without it.

## Consequences
A reviewer or writer could test a proposed README line against a stated rule
instead of a sense of what the file was for, the same shape as CLAUDE.md's
three questions. No ceiling in lines or characters was set: unlike CLAUDE.md,
the README was not loaded on every request, so its cost was adherence rather
than tokens. The cost, accepted knowingly: a reader wanting to know why a field
existed had to open the ADR that created it.
