---
name: ship-a-branch
description: Take a finished branch through to a merged PR — updating against main, cleaning history, writing and maintaining the PR body, handling review rounds, and choosing the merge strategy. Use when a branch is ready for review, or when a review has come back.
---

## When this applies
The work is written and the gate is green. Everything below is procedure for
getting it merged; the *rules* that constrain code on any turn stay in CLAUDE.md.

CLAUDE.md → Git workflow Tier 1 governs every step below. It is not negotiable,
and it is not restated here: read it there.

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
  `git branch -m <new-name>`. **Check for an open PR first: renaming its head
  branch closes it**, by either route — deleting the old name, or GitHub's
  branch-rename API (seen on PR #39). With no PR yet and the branch pushed,
  `git push -u origin <new-name>` then `git push origin --delete <old-name>`.
  With one, the rename means a new PR from the new name and a comment on the old
  one pointing at it, so say so when proposing it. Do this before step 3's
  reviews, not after — a PR opened under a stale name drags the mismatch into
  its title too.
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

The GitHub bot is the exception, since it needs a PR to run at all (its bullet
below says when it runs).

- **Run `/code-review` on every branch but two: a comments-only diff, and a
  docs-only diff outside the governing-document shape** (next subsection). Those
  are step 3's only exemptions. The criterion is what the change can break, not
  what file extension it touches: a workflow, a hook or a build file can break the
  gate, and a governing document is what the agent executes, so it can break
  every later change.
- **`/code-review` is run by the human — the agent cannot invoke it.** So ask and
  wait. Never tick that box, never excuse it, and never confuse it with the
  GitHub bot: they are different reviewers and the bot's quota says nothing about
  whether `/code-review` has run.
- **The GitHub bot is an advisor, not a gatekeeper.** It gets one round per PR,
  the one that starts on its own. Never start another by any trigger
  `docs/project/TOOLING.md` §9 lists, and read its result by the signals listed
  there. Fixes to its findings get the gate and, if the stopping rule earns one,
  a local `/code-review`. If it is unavailable, merge anyway — an external quota
  must not decide whether work ships. It is unavailable once its usage-limit
  comment appears, or when no result has arrived 30 minutes after the trigger
  that started its round, whatever else it has or has not posted.
- **A subagent reviewer's claims about repo or remote state are findings to
  verify, not facts to relay.** Weigh its reasoning; check its assertions.

### A governance diff earns the checklist
`/code-review` judges whether a rule is sound; it does not check one document
against the others, which is where governance drift lives. **If the branch touches
`CLAUDE.md`, any `.md` under `docs/project/` or `.github/`, any Skill
under `.claude/skills/`, or `.claude/settings.json` (ADR 0027), run the *Documentation integrity* detectors from
`docs/project/DRIFT-CHECKLIST.md` before opening the PR, after the last round's
fixes**, and tick the box for
it in the PR body so the run leaves a trace. That shape is deliberate: a list of
governing documents would be one more thing to keep in sync, and mockup and
image assets are excluded because no detector reads them
(`docs/project/decisions/0022-a-governance-diff-triggers-the-checklist.md`).

Run the section as it stands rather than a remembered count of it, scoped as
its header says
(`docs/project/decisions/0026-a-detector-runs-when-its-input-changes.md`).
Say in the PR box which conditional items ran and which were skipped. Confirm
each detector reached everything it claims to cover. A check that silently
skips what it cannot parse reports health it never established: one reported
twenty ADRs clean while reading seventeen.

### Triage every finding into one bucket, and say which
- **Fix** — anything fixed on this branch, in one of two kinds. The table says
  which, because only the first can earn another round (stopping rule below):
  - **Fix (defect)** — a real defect, fixed here unless the human, confirming
    the table, defers it. In code it is correctness, data loss or something the
    user can perceive, and its fix carries a regression test verified per
    CLAUDE.md → Testing Stack. In a governing document it needs a failure
    scenario in which an agent following the text *acts* wrongly, for example
    two rules prescribing different actions, a broken reference, a missing
    step, or a step that cannot be carried out. No test applies; the
    *Documentation integrity* detectors are its check. The row's reason quotes
    that evidence — both clauses, the reference, the step — rather than
    asserting it; a row that cannot quote it is not a defect.
  - **Fix (cheap)** — anything else cheap enough to fix here. Wording,
    duplication, wrap width and friction with no such scenario go here, or to
    Defer on one of its grounds.
- **Defer** — valid, and not fixed on this branch, because it is outside the
  branch's unit of work (CLAUDE.md → Git workflow), a document already defers it
  (the Spec, the accessibility default below, an open issue), or its fix needs a
  plan. A non-defect fix too large to be cheap is one that needs a plan. A
  valid finding that meets none of the three is fixed here, and a real defect
  is deferred only by the human. When the ground is an open issue, link that
  issue, labelled `review-debt` if it was not, instead of opening another.
  Otherwise open an issue labelled
  `review-debt`, link it in the thread when there is one, merge. Every time, not
  when remembered: the label is the only register of what a review found and
  nobody fixed (`gh issue list --label review-debt`).
- **Dismiss** — the finding is wrong. Verify the claim against the repo and
  `gh api` first; the written reason is what that check showed. A bot finding is
  answered in its thread, which is its record (§ Closing threads).

A local `/code-review` has no thread and runs before the PR exists, so its
record is carried by hand. Save each round's raw output verbatim before writing
any digest of it, then its table, outside the repository, as
`YYYY-MM-DD-<topic>-r{k}.raw.txt` and `.triage.md` — never only in the
conversation, which a later session cannot read. Where is the human's choice,
recorded in the agent's memory because it is a path on one machine; if memory
names no place, ask before the first round. §4 copies its deferrals and
dismissals into the PR body's `Deferred:` and `Dismissed:` lines from there.
The table file has this shape; Origin is filled for `Fix (defect)` rows only:

```markdown
# <branch> — round <k> triage (proposed | confirmed by the owner YYYY-MM-DD)
Raw: <raw file> · Commit reviewed: <sha> (+ uncommitted changes, if the tree was dirty)

| # | Finding | Bucket | Origin | Evidence / verified reason |
|---|---|---|---|---|

Stopping rule: <verdict>
Owner decisions (YYYY-MM-DD): <confirmed as proposed, or each change>
```

Accessibility findings are deferred by default (CLAUDE.md → Accessibility),
*except* where a change removed an affordance that already worked — that is not
new accessibility work, it is not regressing, and it is a Fix (defect).

**Propose the triage, then wait.** Write the whole table to its file — every
finding, its bucket, its verified reason, and each `Fix (defect)` row's origin —
before changing anything. In the chat give one line per finding (number, bucket,
a reason of twelve words or fewer), then the verdict and the file's path. Touch
nothing until the human has confirmed the table, every row of it.
The table ends with the round's verdict, `Stopping rule: another round | stop,
on to §4 | stop, back to the human, because …`, which the human confirms with
it: after the fixes the natural next step is amend and push, and nothing there
asks whether a round is owed. Choosing a bucket is plan-level (CLAUDE.md →
Human-in-the-Loop): a defect filed as debt leaves the branch exactly as a
dismissed one does, and a finding moved out on the agent's word alone is a
finding silently dropped — worse than no review, because it looks like coverage.
If a finding exposes code/doc drift, log it per the Self-Healing Loop.

**A false positive that recurs is a gap in the document.** The reviewer reads the
repository, not the records of earlier rounds — PR bodies, threads, the tables
saved outside it — so only the text it does read can stop the same finding coming
back. Fix that text until it answers the question itself. Keep no separate
ledger: when a finding looks familiar, the record is in earlier PRs, open or
merged — their `Deferred:` and `Dismissed:` lines and their review threads — and,
for this branch's own rounds, in the saved tables.

**Stopping rule.** Every `Fix (defect)` row names its origin, spelled out in the
table: `original diff` (already in the diff the first round read),
`from a cheap fix` or `from a defect fix` (introduced by a `Fix (cheap)` or a
`Fix (defect)` of an earlier round), or `other: <what>` (a merge of `main`, new
work between rounds), which counts as `original diff`. Each saved table records
the commit its round reviewed; check the origin with git against those commits.
Then, in this order:
- **No `Fix (defect)` row** (findings cheap, deferred, dismissed or none): stop,
  on to §4.
- **Origin `from a cheap fix`:** revert that cheap fix, unless the human
  chooses another solution. A revert writes no new text, so it earns no round;
  with no other `Fix (defect)` row left, stop, on to §4.
- **Of the rows left, mostly `from a defect fix`:** the rule of three applied to
  rounds — the fixing has become the source of defects. Stop, back to the
  human: redesign, revert that part, cut it from the branch, or §4.
- **Otherwise** the round earns another, with one cap: round three or later is
  earned only by a defect in a file outside the governance shape (§ A
  governance diff earns the checklist), because a prose reviewer always finds
  one more. When the cap denies the round, stop, on to §4, and its fixes follow
  *A fix no round will read*.

Stopping ends the rounds, not the triage: that round's findings still go
through the table above, and a branch stopped back to the human goes no further
until the human decides. The human may always ask for another round; the saved
table says so.

**One redesign per branch.** It restarts the round count for what it rewrote. If
the rule of three fires again on it, there is no second redesign: that part
leaves the branch for a plan (CLAUDE.md → Human-in-the-Loop), and the rest goes
on.

**A fix no round will read** reverts the branch change that caused the defect
where it can, and is otherwise the smallest change that removes it. Re-run the
gate and the detectors after it, and list it under the PR body's Verification
as fixed after the last round, for the human and the GitHub bot, when it runs.
A fix that cannot be small goes back to the human: defer it, cut that part from
the branch, or authorize another round.

**Rule of three:** touching the same code a third time for the same class of
defect means the design is the problem. Stop patching and redesign. A fix that
introduces the next defect in the same place counts double.

### Closing threads
Reply in the thread saying what was done, then **Resolve conversation** — the
reply is the history, the resolve is the filing. An unanswered thread is
indistinguishable from an unnoticed one. `main` is protected with *require
conversation resolution*, so an open thread blocks the merge button mechanically.

Check which commit a bot review ran against before trusting it; it pins to a SHA
and may already be stale.

## 4. Write the PR body
Use `.github/pull_request_template.md`. **Aim for one screen — about 40 lines.**

Three sections carry it: **what changed**, **why**, **how it was verified**. Add
another heading when the PR genuinely needs one (a known limitation, a scope
note, a decision taken deliberately) — but only then; the default is the three.

Replace the `TODO` on the template's `Deferred:` and `Dismissed:` lines with
the local `/code-review` rounds' issues and dismissals, each dismissal as its
round and number, what it claimed and its verified reason, in about twenty
words, so the line still answers a recurring finding once the saved table is
archived. Write `none` when the review ran and produced none, and
`not run — exempt: <the step-3 exemption>` only when step 3 exempted the branch.
A review still to come is not an exemption: the `TODO` stays, visible, until the
record is written.

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
**Ask first (CLAUDE.md → Git workflow Tier 1).** The yes given for opening the PR
does not carry over to the merge.

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
