---
name: review-round
description: Run a code review round — ask for /code-review, save its raw output, triage every finding in a table the human confirms, apply the stopping rule and the cap. Use when a branch is ready for review, when /code-review or the GitHub bot returns findings, or to decide whether another round is owed.
---

# Run a review round

Applies when a branch is clean and its gate green, when `/code-review` or the
GitHub bot returns findings, and when deciding whether another round is owed.
Every local round runs before the PR exists (ADR 0014): a PR opened early anchors
threads to commits the fixes rewrite, and its body goes stale. The bot's findings
take the same triage, and no local round reads their fixes: they follow § A fix
no round will read. Next: `clean-branch` to fold the fixes, then another
round or `open-pr`.

## Stopping rule
Every `Fix (defect)` row names its origin, checked with git against the commits
the saved tables record: `original diff`; `from a cheap fix` or `from a defect fix`,
introduced by an earlier round's fix of that kind; or `other: <what>`, such as a
merge of `main` or new work between rounds, which counts as `original diff`. Then,
in order:
1. No `Fix (defect)` row: stop, on to `open-pr`.
2. Origin `from a cheap fix`: revert that cheap fix, unless the human chooses
   another solution. A revert writes no new text, so it earns no round. The finding
   it answered reopens as a Defer, because its fix needs a plan. With no other
   defect row left, stop, on to `open-pr`.
3. Of the rows left, mostly `from a defect fix`, or any of a class an earlier round
   fixed in the same place: the fixing is not converging. Stop, back to the human:
   redesign, revert that part, cut it from the branch, or `open-pr`.
4. Otherwise another round, up to two `/code-review` rounds per branch, governance
   and product alike (ADR 0049): a reviewer always finds one more, while real
   defects fall. When the cap denies it, stop, on to `open-pr`.

Before proposing a round past the cap, or a redesign, read
`references/past-the-cap.md`. Stopping ends the rounds, not the triage, and a
branch stopped back to the human goes no further until the human decides. The rule
of three counts touches to the code across branches, not rounds, so the cap leaves
its count alone; rule 3 applies it sooner.

## Which branches get a round
The criterion is what a change can break, not its file extension. Every branch
gets a round except:
- a comments-only diff;
- a docs-only diff outside the governance shape (`open-pr` § 1);
- a diff that only adds lines to `.gitignore`, each a literal path with no glob,
  with `git ls-files -ci --exclude-standard` printing nothing: a broader pattern
  or a removed rule can hide a source file from every clone or expose a secret;
- a branch working `review-debt` that the human exempts: read
  `references/review-debt-branch.md`.

An exempt branch records `Review: not run — exempt: <which>` in the PR body.

A branch that ADR 0041 says carries an OpenSpec change and has no folder
`openspec/changes/<branch-kebab-name>/` stops here. One that has it runs `openspec-verify-change`
before the first round and fixes every CRITICAL. Each test a scenario's
`VERIFIED BY` names exists under that name, and each `device` one joins the device
pass.

## Before each round, ask
A round starts only on the human's explicit yes, whoever launches it. Send *ready
for round k* with:
- what the round reads: the commits, and whether the tree has uncommitted changes;
- what is still open in the conversation: unanswered questions, requests not done,
  changes not confirmed, or "nothing";
- the question: launch round k?

An answer that carries a question or a request is not a yes: handle it and ask
again. Note the commit the round reads, for the table. The GitHub bot is a
different reviewer: its round says nothing about whether `/code-review` ran.

## The sequence
1. Save the raw output verbatim, before any digest of it:
   `node .claude/skills/review-round/scripts/save-review.mjs <task-id> <raw file>`,
   the task id from the round's notification. It prints the table's `Reviewer:`
   line. The subagent's output file is always empty and its notification escapes
   `<` and `>`, so neither is the record. If the script fails, read
   `references/extractor-failed.md`.
2. Verify every finding (§ Verifying) and check each `Fix (defect)`'s origin with git.
3. Write the table to its file, marked proposed, with the verdict.
4. Send the message (§ The message) and wait.
5. Record the human's answer under `Owner decisions` with the date, and mark the
   table confirmed.
6. Apply, in order: the reverts the stopping rule asks for; `Fix (defect)`;
   `Fix (cheap)`, only those that pass § A fix no round will read if this round
   ends the rounds; then open or label each Defer's issue. On a branch with an
   OpenSpec change, a fix that changes behaviour, the bot's included, also updates
   the change's delta.
7. Run the gate, and after the last round on a governance diff the detectors too
   (`open-pr` § 1).
8. Fold the fixes into the commits that should have been right (`clean-branch`).
9. Say the verdict first: another round, back to *ready for round k*; `open-pr`;
   or wait for the human. A push or a PR is never mentioned before it.

No digest comes before the raw file, and no change before its row is confirmed
(§ A partial answer). No commit, fold, round or PR comes before every row and the
verdict are confirmed and every open question answered.

A subagent reviewer's claims about the repository or the remote are findings to
verify, not facts to relay: weigh its reasoning, check its assertions.

## Buckets
Choosing a bucket is an ask-first decision: a finding moved out of the branch on
the agent's word alone is silently dropped, and looks like coverage.
- **Fix (defect)**: fixed here unless the human defers it. In code, correctness,
  data loss or something the user perceives, with a regression test seen to fail
  (`.claude/rules/tests.md`). In a governing document, a scenario in which an agent
  following the text acts wrongly: two rules prescribing different actions, a
  broken reference, a missing step, a step that cannot be carried out. The row
  quotes both clauses, the reference or the step; a row that cannot is not a
  defect. Only this bucket can earn a round.
- **Fix (cheap)**: anything else cheap enough to fix here, such as wording,
  duplication, wrap width, or friction with no failure scenario.
- **Defer**: valid and not fixed here, on one ground: it is outside the branch's
  unit of work; a document already defers it (the PRD, an open issue, ADR 0013
  for accessibility); its fix needs a plan; or it is a cheap fix that fails § A
  fix no round will read. A change that removed an affordance that already worked
  is a defect, not deferred accessibility work. Link the open issue that covers
  it, labelled `review-debt`, or open one with that label: the label is the only
  register of what a review found and nobody fixed. Only the human defers a real
  defect, and its row says what it breaks if shipped: who notices, how often,
  whether it can be undone.
- **Accept**: valid, not a defect, not worth fixing, and not followed up, so it
  opens no issue. Always the human's call. A row that can cite a failure scenario
  is a defect and is never accepted.
- **Dismiss**: the finding is wrong. Check the claim against the repository or
  `gh api`; the reason is what the check showed.

A valid finding that fits neither Defer nor Accept is fixed here. A finding that
recurs, dismissed or accepted, is a gap in the text the reviewer reads, since it
never reads earlier records: fix that text until it answers the question. Earlier
PRs' records and threads hold the history.

## Verifying
| Bucket | Before the table | Recorded in Evidence |
|---|---|---|
| Fix (defect), code | Trace the failure to `file:line`, or run it if that is cheap | The regression test seen failing. If it does not fail, the row goes back to the human: the bucket was wrong |
| Fix (defect), governance | Quote both clauses, the reference or the step | The quote; then the detectors |
| Fix (cheap) | The text or code says what the finding claims | Nothing, or in the last round the test that lets it through |
| Defer | `gh issue view` shows the issue open, or a document defers it | The issue number |
| Accept | It is valid and no failure scenario exists | Why it is not worth fixing |
| Dismiss | The claim checked against the repository or `gh api` | The quote or the command |

## The table
Save the raw output and the table outside the repository as
`YYYY-MM-DD-<topic>-r<k>.raw.txt` and `.triage.md`, never only in the conversation,
which a later session cannot read. Where is the human's choice, kept in agent
memory; if memory names no place, ask before the first round. The table is in
English, because the PR record copies it.

```markdown
# <branch> — round <k> triage (proposed | confirmed by the owner YYYY-MM-DD)
Raw: <raw file> · Commit reviewed: <sha> (+ uncommitted changes, if the tree was dirty)
Reviewer: <model id>, <level>

| Id | Claim (file:line) | Bucket | Origin | Evidence |
|---|---|---|---|---|

Stopping rule: another round | stop, on to open-pr | stop, back to the human, because …
Owner decisions (YYYY-MM-DD): <confirmed as proposed, or each change>
```
- The id is `r<k>-<n>`, `n` in the order the reviewer reports, never renumbered; a
  finding split across buckets takes a letter, `r1-2a`. The same id appears in the
  chat, the PR record and any issue.
- A row lets the human judge it without opening the raw file: what the reviewer
  claims, where, its bucket, and the check that decides it. Origin is filled for
  `Fix (defect)` only. Labels are written out in full.
- `Reviewer:` is the line `save-review.mjs` printed: which model at which level
  found what, readable when a round is weighed later.

## The message
The chat never shows the table; it goes in the human's language. One form for
every branch:
```
Round <k> of <branch>, commit <sha>: <n> findings — <a> Fix (defect), <b> Fix (cheap), <c> Defer, <d> Accept, <e> Dismiss.
Verdict: <verdict> (<the rows that decide it>).
To decide:
- r<k>-<n> · <what it claims, 12 words or fewer> · <options> · I recommend <one>
To know:
- r<k>-<n> · <what it claims> · <what the human should know>
Leaves the branch:
- r<k>-<n> · <what it claims> · Defer → #<issue> or a new issue · <reason>
Table: <path>
```
- **To decide:** a defect that would leave the branch; a `Dismiss` whose reason is
  not a quote or a command anyone can repeat; a row whose call between defect and
  cheap decides whether a round is earned; a fix with more than one reasonable
  solution, or one that writes or changes a rule.
- **To know**, a closed list: a fix that changes what the user sees, or what a rule
  means, beyond what the finding asks; one that undoes something the human asked
  for or decided; one that touches a file outside the branch's unit of work. These
  are applied unless the human reopens them.
- **Leaves the branch:** every `Defer` and `Accept`, and a `Dismiss` only when its
  reason cannot be repeated. A checked `Dismiss` is counted, not listed. Each row
  here needs the human's explicit answer: a Defer opens or labels an issue, and
  only the human accepts.
- Leave out an empty section. Every other row stands as proposed unless the human
  reopens it, and the human can always ask for the whole table.

## A partial answer
Rows the human confirmed may be applied, uncommitted, while another row is still
open, unless they touch the same file and section as the open row. The verdict
waits for every row, since the open one may be the defect that earns a round, and
so do commits, folds, rounds and the PR.

## A fix no round will read
Any fix of the round that ends the rounds, and any fix after it.
- A `Fix (defect)` reverts the change that caused the defect where it can, and is
  otherwise the smallest change that removes it. One that cannot be small goes back
  to the human: defer it, cut that part from the branch, or authorize another round.
- A `Fix (cheap)` is applied only if its row names one of two tests; otherwise it
  goes to Defer, all such rows into one `review-debt` issue, except on a branch
  working `review-debt`, which opens no new debt (`references/review-debt-branch.md`). **Proven by a
  mechanism:** a test seen failing on a mutation of what it guards, then passing; it
  changes no governing text, since a reader is that text's only proof, and settles
  no question a deferral left open. **Writes no new rule:** a spelling or wrap fix,
  a revert, a deletion nothing depends on, text moved unchanged, or a pointer to
  something that exists.

Either way, run the gate and the detectors after it, and list it in the PR record
under `Fixed after the last round`.
