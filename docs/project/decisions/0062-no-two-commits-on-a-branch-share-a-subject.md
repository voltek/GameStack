# ADR 0062 — No two commits on a branch share a subject

Status: Accepted
Date: 2026-11-10
Backfilled: no
Enforced by: git hook — .githooks/pre-push — the typed subjects of the commits being pushed, outside the remote-tracking `main` or else the local one, merges left out; with neither, a warning only
Enforced by: commits.yml — the typed subjects of every commit on a pull request, merges left out

## Context
On the branch of PR #190, a reword meant to fold into an earlier commit lost its
`amend!` line: the agent read `clean-branch` with a search instead of invoking
it, missed the step that wrote the marker, and `GIT_EDITOR="cp <file>"`
replaced the line git had written. The result was an ordinary commit whose
subject repeated another on the branch. The hook accepted it, `--autosquash`
left it in place, no error appeared, and a rebase chained to a push sent it to
the remote. Nothing checked whether a subject appeared twice. git runs the
commit-msg hook with no sign of an `--amend`, so a check there would also have
rejected, wrongly, every amend that kept its subject.

## Decision
Every commit on a branch outside `main` has its own subject, merges aside.
Before a push, and on every pull request, reject a branch where two commits in
the subject form share a subject, and point at the reword script. A subject off
the form is left to the form check.

## Consequences
A reword that lost its marker stopped at the push instead of reaching the
remote, and the branch could not go on until the copy was folded. The check ran
at the push, not the commit, so a copy was found after it was made rather than
as it was made. Two commits that made different changes under one subject were
rejected too, which the one-claim-per-commit rule already counted as a defect.
The push measured from `main` as last fetched and never fetched itself, so a
branch that had merged a newer `main` by another route, such as a second remote
or a fetch of one other branch, could be rejected for commits `main` already
held; a fetch of `main` cleared it, and CI measured from the real base.
