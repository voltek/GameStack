# ADR 0034 — Commit subjects, PR titles and branch names share one form

Status: Accepted
Date: 2026-10-26
Backfilled: no
Supersedes: ADR-0006 — "branch names are `feature/`, `fix/`, `chore/` or `docs/{name}`" and "lowercase" (Decision, Tier 2 paragraph)
Enforced by: none (prose only — CLAUDE.md § Git workflow Tier 2) — the subject form (type, scope omitted only when repo-wide, verb-first capitalised summary, no trailing period, 72 characters), the branch name
Enforced by: none (prose only — .claude/skills/ship-a-branch/SKILL.md § 2) — the closed scope list and how it grows, the PR title stating one claim with no "and", the exemption for subjects git or GitHub writes

## Context
ADR 0006 adopted Conventional Commits on 2026-08-22 with a subject that was
"lowercase", and CLAUDE.md described the scope as "the feature or layer". Of the
90 commits authored on `main` from that date to 2026-10-26, 60 were `docs`, and
their scopes named the file touched rather than the area: ten names, several for
one area (`ship` and `ship-a-branch`; `drift`, `checklist`, `decisions` and
`governance`), which made a scope useless as a `git log` filter. "Lowercase" was
applied to identifiers, producing `claude.md` and `adr`. 18 subjects exceeded 72
characters and 12 of those carried two claims. GitHub writes the PR title into
the merge commit's body, yet no rule covered it, and PRs #45 and #49 merged with
an "and" in it. Three branches carried a prefix other than their PR's type, and
none existed for `refactor` or `test`.

## Decision
A commit subject written by hand is `type(scope): Imperative summary`, at most
72 characters, with no trailing period. The summary opens with its verb,
capitalised; identifiers, filenames and proper nouns after it keep their
spelling. Subjects git or GitHub writes itself, for a merge, a revert or a
squash, are exempt.

A scope names the area a change governs, never the file it touches, and comes
from one closed list. A new scope joins the list in the commit that first uses
it. A repo-wide change may omit the scope.

A PR title takes the same form and states the branch's one claim; on a
one-commit branch it is that commit's subject. A title that needs an "and" means
the PR is split.

A branch is named `{type}/{kebab-name}` with its PR title's type, where
`feature/` stands for `feat`.

## Consequences
A scope became a filter: one name per area, and a new name was a visible edit
rather than an accident. The mechanical half became checkable by a script: the
form, the casing, the length and the scope list of every subject, and the shape
of a branch name; the PR title, and its type against the branch prefix, only in
CI, since no local hook sees a PR title. The judgement half stayed prose:
whether the summary is imperative, whether a scope names the area, whether the
title is the branch's one claim. The cost: subjects of a single claim past 72
characters had to be reworded. The capital first letter was the owner's
preference over matching the lowercase subjects already merged, so `git log`
read in a third style from this date, older subjects never rewritten.
