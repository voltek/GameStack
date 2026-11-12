---
name: review-round
description: Run a local /code-review round on the human's yes, triage its findings, and decide whether another is owed. Use when a branch is ready for review before its PR, when /code-review returns findings, or to triage the GitHub bot's findings.
---

# Run a review round

Starts on a clean branch with a green gate. A round here is a local `/code-review`
round, and every one runs before the PR exists (ADR 0014): a PR opened early anchors
threads to commits the fixes rewrite, and its body goes stale. The GitHub bot's round
is external: it runs on the PR (`open-pr` § 5), neither counts as a local round nor
replaces one, and borrows only this triage; its fixes follow § A fix no round will
read. Next: another round or `open-pr`.

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
branch stopped back to the human goes no further until the human decides.

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

An exempt branch writes `Not run — exempt: <which>` under the PR body's *Review*.

A branch that ADR 0041 says carries an OpenSpec change, exempt or not, and has
no folder `openspec/changes/<branch-kebab-name>/` stops here. One that has it runs
`openspec-verify-change` before its first round, or before `open-pr` when it is
exempt, and fixes every CRITICAL. Each test a scenario's `VERIFIED BY` names
exists under that name, and each `device` one is among those the device pass
checked (`verify-screen-on-device`). Not exercised yet (#106): no branch has taken
a change through a round. The first branch to run it corrects it and removes this
note.

## Before each round, ask
A round starts only on the human's explicit yes. Send *ready for round k* with:
- what the round reads: the commits, and whether the tree has uncommitted changes;
- what is still open in the conversation: unanswered questions, requests not done,
  changes not confirmed, or "nothing";
- the level, `low`, `medium` or `high`, with its reason in one line: `low` gives
  fewer, surer findings; go higher as the diff grows or reasons across files;
- the question: launch round k?

On a yes, invoke `/code-review <level> --max-findings default` through the Skill
tool, on the session's model (#187 holds cheaper ones). Always pass both: a level
or a cap typed earlier is reused otherwise. An answer that carries a question or a request is not a yes:
handle it and ask again. Note the commit the round reads, for the table.

## The sequence
1. Save the raw output verbatim, before any digest of it:
   `node .claude/skills/review-round/scripts/save-review.mjs <task-id> <raw file name>`,
   the task id from the round's notification. It saves into the checked-out
   branch's folder (§ The table) and prints the table's `Reviewer:` line and the
   path; write the table beside it. The notification escapes `<` and `>`, so it
   is not the record. Exit 2 is a usage error, which its message names; on exit
   1, read `references/extractor-failed.md`.
2. Verify every finding (§ Verifying) and check each `Fix (defect)`'s origin with git.
3. Write the table to its file, with the verdict.
4. In `/code-review`'s round 1 only, apply alone every `Fix` row that is not for
   *To decide* (§ The message), a code defect only with its regression test seen
   failing, and a cheap one only if it passes § A fix no round will read, unless
   the verdict is another round however the *To decide* rows go. Hold the rest,
   opening no issue. Run the gate and commit each row's fix as its own `fixup!`
   commit, so the human can drop one.
5. Send the message (§ The message) and wait.
6. Record the human's answer and its date on the table's `Owner decisions` line.
   Drop the commit of a row the human reopened, wherever it sits:
   `git revert --no-commit <its sha>`, then `git commit --fixup=<the commit it
   corrects>`, so the fold cancels it.
7. Apply what is left, in order: the reverts the stopping rule asks for;
   `Fix (defect)`; `Fix (cheap)`, only those that pass § A fix no round will read
   if this round ends the rounds; then open or label each Defer's issue. On a
   branch with an OpenSpec change, a fix that changes behaviour, the bot's
   included, also updates the change's delta. Not exercised yet (#106): no delta has
   been updated. The first branch to run it corrects it and removes this note.
8. Run the gate, and after the last round on a governance diff the detectors too
   (`open-pr` § 1).
9. Fold the fixes into the commits that should have been right (`clean-branch`).
10. Say the verdict first: another round, back to *ready for round k*, unless a
    bare yes already answered it in round 1's message; `open-pr`; or wait for the
    human. A push or a PR is never mentioned before it.

No digest comes before the raw file, and no change before its row is confirmed
(§ A partial answer), except step 4's. No other commit, and no fold, round or PR,
comes before every row and the verdict are confirmed and every open question
answered.

## Buckets
Moving a finding out of the branch is the human's call: on the agent's word alone
it is silently dropped, and looks like coverage.
- **Fix (defect)**: fixed here unless the human defers it. In code, correctness,
  data loss or something the user perceives. In a governing document, a scenario
  in which an agent following the text acts wrongly: two rules prescribing
  different actions, a broken reference, a missing step, a step that cannot be
  carried out; a row that cannot quote them is not a defect. Only this bucket can
  earn a round. § Verifying says what proves each.
- **Fix (cheap)**: anything else cheap enough to fix here, such as wording,
  duplication, wrap width, or friction with no failure scenario.
- **Defer**: valid and not fixed here, on one ground: it is outside the branch's
  unit of work; a document already defers it (the PRD, an open issue, ADR 0013
  for accessibility); its fix needs a plan; or it is a cheap fix that fails § A
  fix no round will read. A change that removed an affordance that already worked
  is a defect, not deferred accessibility work. Link the open issue that covers
  it, labelled `review-debt`, or open one with that label: the label is the only
  register of what a review found and nobody fixed. The issue describes each
  finding, never by its id, which resolves to nothing once the tables are
  deleted (ADR 0063). Only the human defers a real defect, and its row says what
  it breaks if shipped: who notices, how often, whether it can be undone.
- **Accept**: valid, not a defect, not worth fixing, and not followed up, so it
  opens no issue. Always the human's call. A row that can cite a failure scenario
  is a defect and is never accepted.
- **Dismiss**: the finding is wrong; the reason is what the check showed.

A valid finding that fits neither Defer nor Accept is fixed here. A finding that
recurs, dismissed or accepted, is a gap in the text the reviewer reads, since it
never reads earlier records: fix that text until it answers the question.

## Verifying
Every check runs before the table; the table records only what a later step
reads (ADR 0071).

| Bucket | Check | Evidence column |
|---|---|---|
| Fix (defect), code | Trace the failure to `file:line`, or run it if that is cheap | The regression test, seen failing. If it does not fail, the row goes back to the human: the bucket was wrong |
| Fix (defect), governance | Quote both clauses, the reference or the step | Empty; the detectors run after the fix |
| Fix (cheap) | The text or code says what the finding claims | Empty, or in the last round the test that lets it through |
| Defer | `gh issue view` shows the issue open, or a document defers it | The issue number |
| Accept | It is valid and no failure scenario exists | Why it is not worth fixing |
| Dismiss | The claim checked against the repository or `gh api` | The quote or the command |

## The table
Save the raw output and the table outside the repository, in `reviews/<branch>/`
beside the handoff (the `checkpoint` Skill's § What is kept), as
`YYYY-MM-DD-r<k>.raw.txt` and `.triage.md`. The file is what survives a
compaction or a new session, which lose the conversation, so round 2 reads its
origins and the PR body its fixes from there. The SessionStart hook deletes the
folder once the branch is gone (ADR 0067). The table is in English, because the
PR body and the issues draw on it.

```markdown
# <branch> — round <k> triage
Raw: <raw file> · Commit reviewed: <sha> (+ uncommitted changes, if the tree was dirty)
Reviewer: <model id>, <level>

| Id | Claim (file:line) | Bucket | Origin | Evidence |
|---|---|---|---|---|

Stopping rule: another round | stop, on to open-pr | stop, back to the human, because …
Owner decisions: pending | YYYY-MM-DD, confirmed as proposed, or each change
```
- The id is `r<k>-<n>`, `n` in the order the reviewer reports, never renumbered; a
  finding split across buckets takes a letter, `r1-2a`. The id lives in the chat
  and the tables only.
- The claim is the message's: 12 words or fewer, with its `file:line`. Origin is
  filled for `Fix (defect)` only, Evidence as § Verifying says. Labels are
  written out in full.
- `Reviewer:` is the line `save-review.mjs` printed.
- The GitHub bot's findings stay on the PR, so its round saves no raw file: its
  table is `YYYY-MM-DD-bot.triage.md`, `Raw:` names the PR and the bot's review,
  `Reviewer:` the bot, and its ids are `bot-<n>`.

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
  cheap decides whether a round is earned; a fix that writes a rule, or changes
  what one requires of an agent, that no document or code pattern settles; one
  that changes what the user sees; one outside the branch's unit of work; after
  round 1, a fix with more than one reasonable solution.
- **To know**, a closed list, applied unless the human reopens it: in round 1, a
  fix chosen among reasonable solutions, as `applied <X> · dropped <Y>`, a few
  words each, the reason only when asked; one that changes what a rule means beyond what the
  finding asks, naming the document that settles it; one that undoes something
  the human asked for or decided.
- **Leaves the branch:** every `Defer` and `Accept`, and a `Dismiss` only when its
  reason cannot be repeated. A checked `Dismiss` is counted, not listed. Each row
  here needs the human's explicit answer: a Defer opens or labels an issue, and
  only the human accepts.
- Leave out an empty section. Every other row stands as proposed unless the human
  reopens it, and the human can always ask for the whole table.
- In round 1 the counts read `<a> applied (<b> Fix (defect), <c> Fix (cheap)),
  <d> held`. With *To decide* and *Leaves the branch* empty and another round
  earned, the message ends with *ready for round 2*, reading the branch once these
  fixups fold; otherwise that follows once the decisions are folded.

## A partial answer
Rows the human confirmed may be applied, uncommitted, while another row is still
open, unless they touch the same file and section as the open row. The verdict
waits for every row, since the open one may be the defect that earns a round.

## A fix no round will read
Any fix of the round that ends the rounds, and any fix after it.
- A `Fix (defect)` reverts the change that caused the defect where it can, and is
  otherwise the smallest change that removes it. One that cannot be small goes back
  to the human: defer it, cut that part from the branch, or authorize another round.
- A `Fix (cheap)` is applied only if its row names one of two tests; otherwise it
  goes to Defer, all such rows into one `review-debt` issue, except on a branch
  working `review-debt`, which opens no new debt
  (`references/review-debt-branch.md`). **Proven by a mechanism:** a test seen
  failing on a mutation of what it guards, then passing; print the mutated line
  and check the file still parses (`bash -n` for a script), since a mutation that
  did not land or broke the syntax proves nothing. It changes no governing text,
  since a reader is that text's only proof, and settles no question a deferral
  left open. **Writes no new rule:** a spelling or wrap fix, a revert, a deletion
  nothing depends on, text moved unchanged, or a pointer to something that exists.

Either way, run the gate and the detectors after it, and list it under the PR
body's *Review*, unless it answers the GitHub bot: its record is the reply in
that thread (`open-pr` § 6, ADR 0072).
