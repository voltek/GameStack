---
name: clean-branch
description: Update a branch against main and shape its history into claims — merge origin/main, fold corrections, write the final commit messages, rename a drifted branch. Use when the work on a branch is done and before a review round, or to fold a review's fixes into earlier commits.
---

# Clean a branch

Applies when the work on a branch is done and the gate is green, and again after
a review round, to fold its fixes. A commit in the middle of the work needs none
of this. Next: `review-round`, or `open-pr` once the rounds are
done or the branch is exempt.

## 1. Update against main
```bash
git fetch origin
git merge origin/main
```
Merge, never rebase, a branch that has been pushed: a rebase replays commits that
never passed the gate, so a `git bisect` can land on one that does not build
(ADR 0008). Run the gate on the merge.

## 2. Check the name
Does the branch name still describe what its commits now contain? The name was a
guess made before the work. If not, rename it before any review:
`git branch -m <new-name>`, and once pushed, `git push -u origin <new-name>` then
delete the old name. Renaming the head branch of an open PR closes the PR (seen
on PR #39), so with one open the rename means a new PR and a comment on the old
one: say so when proposing it.

## 3. One claim per commit
The final history holds one commit per claim: a change someone might later want
to find, revert or bisect to on its own. 400 changed lines or 15 files is a smoke
alarm, not a limit (ADR 0008): past it review quality drops, so look for a second
claim to split off.

A branch is clean when:
- every message names a change worth finding later, never `wip` or `address review`;
- every commit passes the gate on its own, which usually decides their order;
- no commit only fixes an earlier commit of this branch, and nothing is added and
  then removed;
- code and its documentation sit in the same commit.

Fold every correction of your own unmerged work, review fixes included, into the
commit that should have been right. Keep a commit that records a change of
design: patch, patch, then redesign is history worth having, while a weak test
later strengthened is not. The test: does the commit represent a different state
of the system, or of your understanding? Only the first earns a commit.

### Folding
`git rebase -i` needs an editor no agent session can answer, and auto mode denies
`git commit --amend`, so fold with marker commits, at the tip or below it:
- A fix: `git commit --fixup=<the commit it corrects>`.
- A new message: write `amend! <its current subject>`, a blank line and the whole
  new message to a file, then
  `GIT_EDITOR="cp <file>" git commit --allow-empty --fixup=reword:<commit>`.

Then `git rebase --autosquash --rebase-merges $(git merge-base origin/main HEAD)`
(git 2.44 or later). This folds and is not the update step 1 forbids (ADR 0059).
`--rebase-merges` rebuilds each merge of `main` on the same parents, so a commit
below one folds too; a merge that had conflicts asks for them again.
`git diff <tip before> HEAD` stays empty. The local hook accepts the marker
subjects and CI rejects them, so fold before a PR reads the branch; then run the
gate and push with a lease.

## 4. Messages
`.githooks/rules.sh` checks every commit's form and prints the rule, what it saw
and the fix, with the scope list when the scope is wrong: rewrite the message,
never `--no-verify`. What it cannot check:
- The scope names what the change governs, not where it is written: a commit rule
  is `ship` whether it sits in CLAUDE.md, an ADR or a Skill.
- The body, 12 lines at most with trailers and blank lines left out, diagnoses
  the change, not how it was reached. Of each sentence ask:
  would a reader in six months need it to see why the change is right? Is it
  already in CLAUDE.md, a rule file, the PRD or an ADR, so a pointer does? Does it
  narrate the process? Cut what fails; the review's account goes in the PR record.
- A rule that leaves a governing document carries its history into the body of
  the commit that removes it (ADR 0033).
- `Drift:` marks a commit that resolves a drift already on `main`, as a claim of
  its own. A drift this branch introduced and folds away never reached `main` and
  takes no trailer.

## 5. Push
Before the push a review or a PR reads, repeat step 1. `git push --force-with-lease`
may rewrite your own branch until it merges. On an open PR, answer the bot's
threads before rewriting the lines they anchor to (`open-pr`).
