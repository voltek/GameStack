---
name: open-pr
description: Open a pull request and see it through the GitHub bot — documentation-integrity detectors, the PR body and review record, the title check, answering and resolving threads. Use when the review rounds are done or exempt, before any gh pr create, or when the bot has answered on an open PR.
---

# Open the pull request

Applies when the review rounds are done or the branch is exempt, before any
`gh pr create`, and when the GitHub bot has answered on an open PR. If a round is
still owed or a triage table is not confirmed, go back to `review-round`. Next:
`merge-pr`.

## 1. Detectors, on a governance diff
`/code-review` judges whether a rule is sound; it does not check one document
against the others, which is where governance drift lives. If the branch touches a
file the header of `docs/project/DRIFT-CHECKLIST.md` names, run that file's
*Documentation integrity* section, scoped as the header says, after the last
round's fixes (ADR 0022). Confirm each detector reached everything it claims to
cover: a check that silently skips what it cannot parse reports health it never
established. The PR body's detector line says what they found.

## 2. The body
Use `.github/pull_request_template.md`: its comments say what each section holds,
and `commits.yml` checks the form and names the rule when it fails (ADR 0057).
What the check cannot see:
- The body reads in a minute. Point at an ADR or the commits instead of restating
  them, since the copy in the body is the one nobody updates. A *What changed*
  that needs more than three sentences usually means a second claim.
- The record copies the saved tables: the counts, each `Deferred:` issue, and each
  `Accepted:` and `Dismissed:` as its id, what it claimed and the reason, in about
  twenty words, so the line still answers a recurring finding once the tables are
  gone. Write `none` where a round left nothing. A round still to come is not an
  exemption: the `TODO` stays, and the check stays red, until the record is written.
- Each issue the branch ships takes its own `Closes #n` under *Issues*, and the
  branch repoints every document that cites that issue to where its content now
  lives (ADR 0038).
- Never replace an open PR's body wholesale with `gh pr edit --body`: it deletes
  the screenshots the human dragged in. Read the body and change only what needs
  changing (DRIFT-LOG, 2026-08-22).

## 3. The title
The title takes the commit subject's form and states the branch's one claim; on a
one-commit branch it is that commit's subject, and GitHub writes it into the merge
commit. A title that needs an "and" carries two claims: split the PR, unless the
"and" belongs to one name.

No local hook sees the title or the body, so check both before opening:
`sh .githooks/rules.sh title "<title>"` and
`sh .githooks/rules.sh body <file> "<title>"`.

## 4. Open it
Every PR action GitHub shows needs the human's yes. Open the PR ready for review,
never as a draft: the bot's round starts when it opens.

Editing a PR's title or body reruns `commits.yml` on the same commit. Until the new
run ends, `gh pr checks` and `mergeStateStatus` read the old one, so a `BLOCKED`
then is not a protection fault (#75).

## 5. The GitHub bot's round
The bot is an advisor with one round per PR, the one that starts when the PR
opens. `docs/project/TOOLING.md` § 9 lists its triggers and signals; never start a
second round by any of them. Its clock starts when the PR opens: it is unavailable
once its usage-limit comment appears, or when no result has arrived 30 minutes
later, whatever else it has posted (ADR 0058). Then go on to `merge-pr`, because an
external quota must not decide whether work ships.

Its findings go through `review-round`'s triage, and no local round reads their
fixes: each follows *A fix no round will read* there, then gets the gate, and the
detectors again if it touched a governing document. A result pinned to an older
commit changes nothing: each finding is checked against the current text, and no
second round runs.

## 6. Threads
Reply in each thread saying what was done, then resolve it: the reply is the
history and the resolve is the filing, while an unanswered thread reads as an
unnoticed one. `main` requires every conversation resolved, so an open thread
blocks the merge; a thread that arrives after the 30 minutes is answered and
resolved too. Reply before a push rewrites the line a thread anchors to: the
thread survives as outdated and still resolves, but loses its place in the diff.
