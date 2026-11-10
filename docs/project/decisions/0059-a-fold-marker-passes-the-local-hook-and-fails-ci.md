# ADR 0059 — A fold marker passes the local hook and fails CI

Status: Accepted
Date: 2026-11-09
Backfilled: no
Supersedes: ADR-0034 — "Subjects git or GitHub writes itself, for a merge, a revert or a squash, are exempt" (Decision)
Enforced by: git hook — .githooks/commit-msg — the closed list of exempt subjects: git's merge subject on a merge commit, `Revert "…"`, `Reapply "…"`, `Squashed commit of the following:`, and the fold markers `fixup!`, `squash!` and `amend!`, an `amend!`'s replacement message checked
Enforced by: commits.yml — a fold marker on any commit of a pull request fails the subject form
Enforced by: none (prose only — .claude/skills/clean-branch/SKILL.md § Folding) — folding with marker commits and `git rebase --autosquash --rebase-merges` onto the merge-base with `main`

## Context
Folding a fix into an earlier commit took `git rebase -i`, whose editor no agent
session can answer, and auto mode denied `git commit --amend`. `clean-branch`
replayed commits by hand with `cherry-pick -n` instead, and round 1 of
`docs/ship-by-moment` found that recipe broke once the branch held a merge of
`main`. git 2.44 ran `--autosquash` without an editor, but the commit-msg hook
rejected the `fixup!` subjects it needed. ADR 0034 exempted subjects "git or
GitHub writes itself, for a merge, a revert or a squash", and its live summary,
"the exemption for subjects git or GitHub writes", read as open: the agent
proposing this change took it to cover `fixup!`, while the Decision's list and
`rules.sh` were closed, and its "squash" meant `git merge --squash`'s message.

## Decision
A subject is exempt from the form only when it is on this closed list: git's
merge subject on a merge commit, `Revert "…"`, `Reapply "…"`, and
`Squashed commit of the following:`. Every other subject takes the form.

A fold marker, `fixup!`, `squash!` or `amend!`, is exempt in the local commit
hook only, and CI rejects it, so none reaches a pull request. Fold with marker
commits and `git rebase --autosquash --rebase-merges` onto
`git merge-base origin/main HEAD`, which keeps the merges of `main`. Folding is
not the update ADR 0008 forbids.

## Consequences
One recipe replaced two, and git did the reordering the manual one got wrong.
The hook and CI disagreed on purpose for the first time: a commit the hook
accepted could fail CI, and a branch pushed with its markers unfolded failed only
when a pull request opened, since `pre-push` checked the branch name alone.
Rebuilding each merge of `main` on its own parents let a fix fold into a commit
below it, and a merge that had needed a conflict resolution asked for it again.

The exemption became a list a reader could not widen by reading a summary:
ADR 0034's clause pointed here, and its live line stopped naming it.
