---
name: merge-pr
description: Merge an approved pull request and clean up — check the body against what changed since the PR opened, archive its OpenSpec change, use a merge commit, delete the local branch, run the checkpoint. Use before any gh pr merge, or when the owner says to merge.
---

# Merge the pull request

An abandoned branch needs only the `checkpoint` Skill. Next: `checkpoint`.

## 1. Before asking
- A branch with `openspec/changes/<branch-kebab-name>/` archives it first, so the
  body can name the archive: read `references/openspec-archive.md`.
- Find out whether the PR changed after it opened. `commits.yml` runs when a PR
  opens, on every push and on every edit of its title or body, so exactly one
  run means it did not change:
  ```bash
  gh run list --workflow commits.yml --branch <branch> --limit 100 --json headSha --jq length
  gh pr view <number> --json closingIssuesReferences --jq '[.closingIssuesReferences[].number]'
  ```
  With any other count, re-read the whole body against the branch: a merge of
  `main` or a fix can leave *What changed* or *Evidence* describing what was
  proposed, not what was done. Correct it in place, never with `--body`
  wholesale. With one run, the body passed `commits.yml` on this same head: check only that the
  closing issues are exactly those the branch ships, since each one closes at the
  merge and nobody reads it again.
- Every thread is answered and resolved, late ones included (`open-pr` § 6).

Then ask: the yes given for opening the PR does not carry over to the merge.

## 2. Merge
`gh pr merge <number> --merge`. A merge commit keeps the curated commits, which
make `blame` and `bisect` useful, and records what landed together as one reviewed
unit (ADR 0008).

Squash only a branch whose history is disposable: it keeps the message text and
loses each commit's SHA, line-level `blame` and every bisection point but one. Keep
every `Drift:` and `Bypassed:` line in the squash message (ADR 0033): in a squash of
several commits they stop parsing as trailers, and `git log --grep='^Drift:'` still
finds them.

## 3. Clean up
GitHub deletes the remote branch at the merge, with a lag (PR #55), because the
repository has *Automatically delete head branches* on.
```bash
git fetch --prune
git checkout main
git merge --ff-only origin/main
git branch -d <branch>
```
If `git branch -r` still lists the branch after another `git fetch --prune`, the
setting is off or a protection rule or ruleset covers the branch
(`gh api repos/{owner}/{repo} --jq .delete_branch_on_merge`, from Bash): tell the
owner.

`-d` refuses unless the branch is merged into `main`. That is the squash case, where
the branch holds the only copy of its commits, so a refusal is information: find
out why before reaching for `-D`, which disables the check.

## 4. Checkpoint
Run the `checkpoint` Skill: the merge changed a cycle status, and it says whether a
triage is owed. End the session there.
