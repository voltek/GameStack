---
name: ship-a-branch
description: Take a finished branch through to a merged PR — updating against main, cleaning history, writing and maintaining the PR body, handling review rounds, and choosing the merge strategy. Use when a branch is ready for review, or when a review has come back.
---

## When this applies
The work is written and the gate is green. Everything below is procedure for
getting it merged; the *rules* that constrain code on any turn stay in CLAUDE.md.

CLAUDE.md → Git workflow Tier 1 is not repeated here and is not negotiable:
always ask before `git push` and before opening a PR, never `--no-verify`, never
force-push `main`, never commit to `main` directly, and the gate is all three of
`build`/`test`/`lint` green.

## 1. Update the branch against main
**Use `git merge main`. Do not rebase a branch that has been pushed.**

```bash
git fetch origin
git rev-list --left-right --count origin/main...main   # check BOTH directions
git merge main
```

Rebase rebuilds each commit on a new base, so the commits that get replayed are
not the ones that passed the gate — only the final tip is ever compiled. A
`git bisect` can then land on an intermediate commit that does not build. Merge
keeps every tested commit intact and adds exactly one new state, which you do
compile.

Rebase is not wrong in general — "rebase to update, squash to merge" is a common
and coherent industry standard that optimises for a `main` that reads as a list
of features. This project optimises for something else: its history is study
material (Spec → *Why this project exists, and what it optimises for*). That is
the whole reason for the difference; it is not a claim that rebase is inferior.

`git rebase main` is acceptable on a branch that has **never been pushed**, where
no SHA is shared and nothing can be orphaned. It is cosmetic there, and optional.

## 2. Check the branch is one claim
- Does every commit exist *because of* this branch's unit of work? If not, park
  it (`git stash`) or `git cherry-pick` it onto a fresh branch off `main`.
- **400 changed lines or 15 files is a smoke alarm**, not a limit. Past roughly
  that size review quality drops sharply. If the PR title needs an "and", split it.
- A branch is clean when every commit message names a change worth finding later
  (no `wip`, no `address review`), every commit passes the gate on its own, no
  commit exists only to fix an earlier commit *on this branch*, and nothing is
  added and then removed within the branch.

Clean it with `git commit --amend` or `git reset --soft` and recommit;
`git rebase -i` is unavailable in this environment.

### The force-push window
`git push --force-with-lease` is allowed to clean **your own** feature branch
(never bare `--force`), and **the window closes at the first review, not the
first push.** Once a bot or `/code-review` has commented, rewriting commits
orphans those threads. After that the only remedy for a messy branch is
`Squash and merge`.

Check it, do not assume it: `gh pr view <n> --json reviews,comments`. A quota
notice from a bot is not a review.

## 3. Write the PR body
Use `.github/pull_request_template.md`. **Aim for one screen — about 40 lines.**

Three sections carry it: **what changed**, **why**, **how it was verified**. Add
another heading when the PR genuinely needs one (a known limitation, a scope
note, a decision taken deliberately) — but only then; the default is the three.

Keep it short by *pointing* rather than repeating. The deep reasoning is already
written twice — in the commit messages and in DRIFT-CHECKLIST's Resolution log —
and the copy in the PR body is the one nobody updates. Same `never both` rule as
code comments.

**A body that needs more than one screen usually means the PR has more than one
claim.** Treat length as the same smoke alarm as 400 lines / 15 files, measured
in prose.

### Screenshots
UI changes need them. Generate locally, save outside the repo named for what
they show (never in `%TEMP%` or a scratchpad — those get cleaned), then **give
the paths in chat** and say plainly the PR needs them dragged into the body's
edit box on the web before merge. Never commit emulator captures.

An agent that verified on device and then let the human go hunting for a
screenshot wasted the one part only the human can finish.

### Never overwrite an open PR body wholesale
`gh pr edit --body` replaces everything, including screenshots the human dragged
in. Read the current body first and change only what needs changing, or add a
comment instead — this has already destroyed one (Resolution log, 2026-08-22).

## 4. Reviews
- **Run `/code-review` on any PR that changes compiled behaviour.** The criterion
  is what the change can break, not what file extension it touches: a
  comments-only or docs-only diff skips it.
- **`/code-review` is run by the human — the agent cannot invoke it.** So ask and
  wait. Never tick that box, never excuse it, and never confuse it with the
  GitHub bot: they are different reviewers and the bot's quota says nothing about
  whether `/code-review` has run.
- **The GitHub bot is an advisor, not a gatekeeper.** Request it once per PR when
  the PR is ready. If it is unavailable, merge anyway — an external quota must
  not decide whether work ships. Watch for its usage-limit comment and stop
  waiting the moment it appears.
- **A subagent reviewer's claims about repo or remote state are findings to
  verify, not facts to relay.** Weigh its reasoning; check its assertions.

### Triage every finding into one bucket, and say which
- **Blocking** — correctness, data loss, a defect the user can perceive. Fix
  before merge, with a regression test verified per CLAUDE.md → Testing Stack.
- **Non-blocking** — design, performance, polish, debt. Open an issue, link it in
  the thread, merge.

Accessibility findings are non-blocking by default (CLAUDE.md → Accessibility),
*except* where a change removed an affordance that already worked — that is not
new accessibility work, it is not regressing, and it is blocking.

Every finding is either fixed or dismissed **with a written reason**. A review
whose findings are silently dropped is worse than none, because it looks like
coverage. If a finding exposes code/doc drift, log it per the Self-Healing Loop.

**Stopping rule:** a round producing only design or style suggestions is the
signal to merge. A round producing a real defect earns another round.

**Rule of three:** touching the same code a third time for the same class of
defect means the design is the problem. Stop patching and redesign. A fix that
introduces the next defect in the same place counts double.

### Closing threads
Reply in the thread saying what was done, then **Resolve conversation** — the
reply is the history, the resolve is the filing. An unanswered thread is
indistinguishable from an unnoticed one. `main` is protected with *require
conversation resolution*, so an open thread blocks the merge button mechanically.

Check which commit a bot review ran against before trusting it; it pins to a SHA
and may already be stale. Re-request with `@codex review` after pushing fixes.

## 5. Before merging, re-read the PR body
A PR open for days describes what was *proposed*, not what was done — a body
naming a mechanism the branch went on to delete has happened here already. Update
it surgically; never `--body` wholesale if the human has added screenshots or text.

## 6. Merge
**`Create a merge commit` is the default.** The branch's commits are curated and
are the unit that makes `blame` and `bisect` useful; the merge commit records
what landed together as one reviewed unit. Delete the branch afterwards.

- **`Squash and merge`** is the fallback for a branch whose history is genuinely
  disposable, or the only remedy once the force-push window has closed on a messy
  branch. It keeps the message *text* and destroys the structure: per-commit
  SHAs, line-level `blame`, and every bisection point but one.
- **`Rebase and merge`: never.** Same reason as §1, at merge scale.
