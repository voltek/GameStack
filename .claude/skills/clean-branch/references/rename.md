# Renaming a branch

Read when § 2 finds that the branch name no longer describes its commits.

Rename it with `git branch -m <new-name>`. Once it has been pushed, run
`git push -u origin <new-name>` and then delete the old name on the remote.
In the same session, move `reviews/<old-name>/` and `captures/<old-name>/`
beside the handoff to the new name: the next session start deletes a folder
whose branch is gone (ADR 0067).

Renaming the head branch of an open PR closes the PR (seen on PR #39). With one
open, the rename means a new PR and a comment on the old one: say so when
proposing it.
