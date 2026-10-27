# ADR 0028 — The agent pushes its own branch without asking; `main` is guarded mechanically

Status: Accepted
Status: Superseded by ADR-0033 — "and log a DRIFT-LOG row" (Decision, second paragraph); the rest of that paragraph stands
Date: 2026-10-23
Backfilled: no
Enforced by: branch protection — `required_pull_request_reviews` is set, `required_status_checks=build / test / lint` and `enforce_admins=true` (together they reject a direct push to `main` from everyone, administrators included)
Enforced by: repository settings — `allow_auto_merge=false` (no push can turn into a merge)
Enforced by: claude settings — `deny` on a push naming `main`; on changing branch protection, which keeps the emergency exit the owner's; on `gh repo edit`, `rename`, `archive` and `delete`, `gh workflow disable` and `gh secret`; and on printing the GitHub token (the agent only, common spellings)
Enforced by: claude settings — `ask` on the common PR actions (create, edit, comment, review, ready, close, reopen, merge, update-branch), on deleting a remote branch, on `git -C` with `push` or `commit`, and on mutating `gh api` calls, which include turning auto-merge on and other repository settings (the agent only, common spellings)

## Context
CLAUDE.md had told the agent to ask before every `git push` since its first
commit, with no reason recorded and no ADR. The owner judged the prompt
irrelevant, and the local settings already allowed pushes, so the document and
the harness disagreed. An analysis of what the prompt protected found one job no
other mechanism did: keeping commits off `main`. Branch protection did not do
it, because `enforce_admins` was off and the agent pushed with the owner's
administrator credentials. A probe on a throwaway branch on 2026-10-23 showed
GitHub rejecting an administrator's direct push with `enforce_admins` on and
accepting it, as "Bypassed rule violations", with it off.

## Decision
Push your own feature branch without asking. Ask before every PR action that
GitHub shows (opening, editing, commenting, reviewing, marking ready, closing,
reopening, merging) and before deleting a remote branch.

`main` binds administrators too: `enforce_admins` stays on. The emergency exit
belongs to the owner alone: turn `enforce_admins` off, make the change, turn it
back on, and log a DRIFT-LOG row. The agent never changes branch protection,
auto-merge or any repository setting.

Auto-merge stays off, so a push can never complete a merge.

## Consequences
The per-push prompt was gone, and the guard on `main` moved to GitHub, where it
held regardless of what ran the command. The cost was that the owner could no
longer merge past a red gate, such as a cache failure unrelated to the diff,
without using the exit. The agent's `deny` patterns were accepted as a second
layer only: they matched one spelling of each command, and both the Bash and
PowerShell tools needed their own copy.
