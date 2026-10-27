# ADR 0032 — A Skill's procedure parameter is not a decision

Status: Accepted
Date: 2026-10-26
Backfilled: no
Enforced by: none (prose only — decisions/0032-a-skill-parameter-is-not-a-decision.md § Decision)

## Context
ADRs 0025 and 0029 kept their procedures in `ship-a-branch` alone, and
`decisions/README.md` admitted "a decision that is born or changes", but no
document said where a procedure ended and a decision began. Review rounds on
PRs #46 and #48 asked whether a change to `ship-a-branch` (a PR-body length
target, a sentence count) needed an ADR. Each time the finding was dismissed,
and each time the next reviewer, who reads the repository and not earlier
dismissals, raised it again (issue #47).

## Decision
A change to a Skill is an ADR when it makes the Decision of an ADR in force
false or incomplete, or when it changes what is allowed, required or forbidden,
or who decides: a merge without a review, a push without asking, a step the
human no longer confirms.

A change to how an allowed step is done — a length target, the order of steps,
a template, a command — is a procedure parameter. Edit it in the Skill alone,
with no ADR.

Test the first criterion by reading the change against the Decision of every
ADR in force that names the Skill
(`grep -l <skill> docs/project/decisions/[0-9]*.md`), skipping clauses a later
ADR superseded. The second has no search: judge it by reading the change.

## Consequences
A reviewer found the answer in the text instead of in a dismissal it could not
see. Skills could be tuned without a record each time. The cost, accepted
knowingly: a parameter's history lived only in commit messages, and an ADR that
governed a Skill's step without naming the Skill could be missed by the search.
