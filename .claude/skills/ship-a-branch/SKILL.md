---
name: ship-a-branch
description: Take a finished branch through to a merged PR — updating against main, cleaning history, writing and maintaining the PR body, handling review rounds, and choosing the merge strategy. Use when a branch is ready for review, or when a review has come back.
---

## When this applies
The work is written and the gate is green. Everything below is procedure for
getting it merged; the *rules* that constrain code on any turn stay in CLAUDE.md.

CLAUDE.md → Git workflow Tier 1 is not repeated here and is not negotiable:
always ask before `git push`, before opening a PR, and before merging it; never
`--no-verify`, never force-push `main`, never commit to `main` directly; and the
gate is all three of `build`/`test`/`lint` green.

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
- **Does the branch name still describe what it now contains?** A branch drifts
  in scope more than its name gets revisited — the name was a guess made before
  the work existed, so nothing forces it to still be true at the end. If the
  final commits are about something else, rename before doing anything below:
  `git branch -m <new-name>`. If already pushed, `git push -u origin <new-name>`
  then `git push origin --delete <old-name>`. Do this before step 3's reviews,
  not after — a PR opened under a stale name just drags the mismatch into the
  PR title too.
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

### Shape the branch into claims, not into a diary
A branch's final history is **one commit per claim**. A claim is a change someone
might later want to find, revert, or bisect to on its own — not a step you went
through to get there.

- **Fold in every correction of your own not-yet-landed work.** Review rounds are
  the common case: five rounds do not mean five commits. Fix, re-run the gate,
  and amend or `reset --soft` into the commit that should have been right.
- **Do not group by `type:` prefix mechanically.** Two `docs:` commits on one
  branch usually mean one claim split in two — fold them. But `feat(search)` and
  `feat(home)` are two claims and stay two, and a branch with genuinely separate
  doc claims keeps them separate. Type is a useful smell, not the rule.
- **Keep a commit that records a change of design.** Patch, patch, then redesign
  is history worth having; "I wrote a weak test, then strengthened it" is not.
  The test: does this commit represent a different state of the system, or a
  different state of my understanding? Only the first earns a commit.
- Every commit still has to pass the gate on its own, which usually decides the
  order: a production fix lands before the test that depends on it.

Typical end state for a feature branch: the production change, its tests, and the
documentation — three commits, however many rounds it took to get there.

### Commit messages
Subject: `type(scope): imperative summary`, lowercase, no trailing period.

Body: **12 lines or fewer.** This project deliberately puts the *diagnosis* in
commit messages rather than a summary of the diff (Spec → *Why this project
exists*), which makes them longer than the usual convention — but that licenses
the reasoning the diff cannot show, not a retelling of it. Three tests:

- Would a reader six months from now need this sentence to see why the change is
  right? If not, cut it.
- Is it already in CLAUDE.md, the Spec, DRIFT-LOG or an ADR? Point at it.
- Is it narrating the process ("first I tried X, then Y", "the reviewer found
  this, I verified that")? Cut it. The review's account belongs in DRIFT-LOG;
  the commit says *what is now true and why*.

The diagnosis is of **the change**, not of how the change was arrived at. That
distinction is what keeps 12 lines enough.

### The force-push window
`git push --force-with-lease` is allowed to clean **your own** feature branch
right up to the merge — never bare `--force`, never `main`, and never a branch
another person may hold.

The window used to close at the first review, to protect bot threads from being
orphaned. That trade was backwards: branch history is permanent and drives
`blame` and `bisect`, while a review thread is read once and effectively never
revisited after merge. Freezing the permanent thing to protect the ephemeral one
also meant a single clumsy commit cost a whole branch's granularity.

Measured on PR #12 rather than assumed. A thread whose commented line was
rewritten keeps its text and `path`, keeps `originalLine` as a reference, loses
`line` (the live anchor becomes null) and is flagged `isOutdated`. It still
**accepts replies** and still **resolves** — and resolving it moved the PR from
`BLOCKED` to `CLEAN`, so this does not deadlock against *require conversation
resolution*.

The only loss is the diff anchor: GitHub can no longer show the comment beside
the code that prompted it. **Reply in the thread before rewriting** — the reply
is the history, the anchor is only the presentation.

In practice this should rarely come up, because step 3 keeps the reviews ahead of
the PR. It exists for the residual case: the bot finding something real on a
branch that was already final.

## 3. Reviews — all of them, before the PR exists
`/code-review` runs against the local branch and needs no PR. Run every round the
stopping rule earns, fold each round's fixes into the commits that should have
been right (step 2), and only then open a PR.

Opening one early costs three things at once: review threads anchor to SHAs that
later get rewritten, the force-push window closes so the branch can no longer be
cleaned, and the PR body starts going stale from the moment it is written. It
also makes step 2 impossible — no PR means the window stays open, and an open window
is what lets a branch be folded into its final shape. The two rules hold each
other up.

The GitHub bot is the exception, since it needs a PR to run at all: request it
once, when the PR opens, after the local rounds are done.

- **Run `/code-review` on any branch that changes compiled behaviour.** The
  criterion is what the change can break, not what file extension it touches: a
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

## 4. Write the PR body
Use `.github/pull_request_template.md`. **Aim for one screen — about 40 lines.**

Three sections carry it: **what changed**, **why**, **how it was verified**. Add
another heading when the PR genuinely needs one (a known limitation, a scope
note, a decision taken deliberately) — but only then; the default is the three.

**`Why` defaults to one paragraph** — three to five sentences pointing at the
decision, not narrating how it was reached. Expand past that only when the
decision has real nuance a reviewer can't get from the diff (a rejected
alternative, a deferred consequence) — and even then prefer pointing at the
commit message, DRIFT-LOG or the ADR over restating them here.

Keep it short by *pointing* rather than repeating. The deep reasoning is already
written twice — in the commit messages and in DRIFT-LOG —
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
comment instead — this has already destroyed one (DRIFT-LOG, 2026-08-22).

## 5. Before merging, re-read the PR body
A PR open for days describes what was *proposed*, not what was done — a body
naming a mechanism the branch went on to delete has happened here already. Update
it surgically; never `--body` wholesale if the human has added screenshots or text.

## 6. Merge
**Ask before merging (CLAUDE.md → Git workflow Tier 1).** This is not covered by
having already asked before push or before opening the PR — the merge is its own
point of no return and needs its own explicit yes.

**`Create a merge commit` is the default.** The branch's commits are curated and
are the unit that makes `blame` and `bisect` useful; the merge commit records
what landed together as one reviewed unit. The repo has **Automatically delete
head branches** enabled (GitHub → Settings → General → Pull Requests), so the
remote branch is cleaned up on its own.

- **`Squash and merge`** is the fallback for a branch whose history is genuinely
  disposable, or the only remedy once the force-push window has closed on a messy
  branch. It keeps the message *text* and destroys the structure: per-commit
  SHAs, line-level `blame`, and every bisection point but one.
- **`Rebase and merge`: never.** Same reason as step 1, at merge scale.

### Then delete the local branch — `-d`, never `-D`
The remote half is automatic; the local ref is the half that is not, and that
asymmetry is what leaves `git branch` reading as archaeology instead of a list of
live work. Order matters, because doing it backwards produces the mistake the
rule exists to prevent:

```bash
git fetch --prune          # drops the remote-tracking refs GitHub already deleted
git checkout main
git merge --ff-only origin/main
git branch -d <branch>
```

`-d` refuses unless the branch tip is an ancestor of `main`. That is exactly the
**squash-merge** case above, where the branch holds the only copy of its granular
history and deleting it makes those commits unreachable. So a refusal is
information, not an obstacle — and `-D` is the key that disables the check. If
`-d` refuses, find out why before reaching for it.
