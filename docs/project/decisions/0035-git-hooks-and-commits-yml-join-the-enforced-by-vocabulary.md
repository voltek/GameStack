# ADR 0035 — Git hooks and `commits.yml` join the `Enforced by:` vocabulary

Status: Accepted
Date: 2026-10-27
Backfilled: no
Enforced by: none (prose only — decisions/README.md § `Enforced by:` — closed vocabulary) — the two values, `(not a required check)` on a `commits.yml` line while that is the case

## Context
ADR 0034 split the message rules into a mechanical half a script could check
and a judgement half that stayed prose, and the script was built as git hooks
under `.githooks/` plus a pull request workflow, `commits.yml`. The `Enforced
by:` vocabulary had no value for either. A git hook binds a person as well as
the agent, unlike `claude settings`, but only in a clone where
`core.hooksPath` points at `.githooks`, and `--no-verify` skips it. A workflow
binds a merge only when branch protection lists its check as required, which
only the owner can set (ADR 0028), and `commits.yml`'s check did not exist
until its first run on a pull request.

## Decision
Add two values to the closed vocabulary:

- `git hook — .githooks/<hook>`, naming the hook that runs the check. Any
  Gradle build installs the hooks, so the value claims no more than a clone
  that has built; where a clause must also hold on `main`, a `commits.yml` line
  covers it too.
- `commits.yml`, like `gate.yml`. While its check is not required in branch
  protection, the line says `(not a required check)` after the value.

## Consequences
The map could say which message rules stopped a commit before it existed and
which were caught again before a merge. The cost was two values weaker than
`gate.yml` while the check was optional: a red `commits.yml` was a signal, not
a block, until the owner made it required and its lines dropped the qualifier.
