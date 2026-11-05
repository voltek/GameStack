# ADR 0049 — Review rounds are capped and the human confirms the triage

Status: Accepted
Date: 2026-11-04
Backfilled: no
Supersedes: ADR-0015
Supersedes: ADR-0025
Supersedes: ADR-0029
Supersedes: ADR-0030
Enforced by: branch protection — `required_conversation_resolution=true` (every review thread resolved before the merge; "answered" is prose; for everyone, administrators included, while `enforce_admins=true`)
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Testing Stack — a regression test that was never seen to fail
Enforced by: none (prose only — CLAUDE.md § Working here) — the rule of three
Enforced by: none (prose only — .claude/skills/ship-a-branch/SKILL.md § Triage every finding into one bucket, and say which) — the human confirms the triage
Enforced by: none (prose only — .claude/skills/ship-a-branch/SKILL.md § 3. Reviews — all of them, before the PR exists) — `/code-review` run by the human
Enforced by: none (prose only — .claude/skills/ship-a-branch/SKILL.md § 3. Reviews — all of them, before the PR exists) — the GitHub bot as an advisor, the exemptions, the buckets and their grounds, the stopping rule, the cap, the round past it, a fix no round will read, a branch working `review-debt`, the rows that go to the human, the records
Enforced by: none (prose only — .github/pull_request_template.md § Verification) — the `Deferred:` and `Dismissed:` lines

## Context
The review policy had been patched clause by clause across ADRs 0015, 0025, 0029
and 0030, so today's rule took four files to read, and `ship-a-branch` § 3
restated it. ADR 0029 capped governance text at two rounds, while the owner's
later rulings lived only in agent memory and an out-of-repository plan: three
rounds with the third verifying, then on 2026-11-03 two for every branch. Across
the recorded triages findings held at about nine per round while real defects
fell from about three to under two, and 36 of the 156 defects came from an
earlier round's fix. Fixes after the last round had no test: a cheap one
followed the agent's judgment. A branch working `review-debt` faced a full
review on top of the rounds that deferred the finding, and the one branch that
closed such an issue with a rule (PR #49) ran two rounds that found six defects.

## Decision
Triage every review finding in a table the human confirms before anything
changes: fixed, deferred to an issue, or dismissed, with the reason recorded.
On a branch that is wholly governance the human decides the rows that turn on a
judgment, and the others stand unless reopened. Answer and resolve every thread.

A real defect, never a finding as such, earns another round, and every branch's
rounds are capped, governance and product alike. Past the cap a round runs only
when a redesign earns it or the human authorizes it. The agent may propose one
only for a failure stated to a defect's standard that no mechanism can check.

A fix no round will read proves itself without a reader or writes no new rule;
a defect's fix reverts or stays minimal. What fails that test becomes debt.

Route a branch working a review's leftovers by its weakest fix, and let it open
no debt of its own beyond a defect the human defers.

The rule of three stops the patching. `/code-review` is run by the human. The
GitHub bot is an advisor with one round per PR, skipped when unavailable. Every
regression test is seen to fail against the code it guards.

The procedure and its numbers live in `ship-a-branch` § 3, never here.

## Consequences
The cap was set at two rounds, one on a branch working `review-debt`. Today's
review policy read from one file. It piloted #162's principle-plus-pointer
form and its consolidation of a chain into one whole supersession.

The cost was accepted knowingly: 17 of the 113 original-diff defects in the tally
were found only in round 3 or later, so a two-round cap would have shipped them
unless a third round was authorized. About 6% of cheap fixes had become defects,
which is why a last-round cheap fix took a test rather than a judgment. A
`review-debt` branch that skipped review rested on the gate, the detectors and
the human's reading of the PR.

Three options lost. Three rounds with the third verifying spent a full round on
every governance branch, at the same nine findings and the fewest defects. A
third round only when a fix could become a defect could not flag the 17 of 35
defects from round 3 or later that came from the original diff. No review at all
on a `review-debt` branch would have shipped the four original-diff defects of
PR #49, which wrote a rule.

The rule of three kept its number, because it counted touches to the code across
branches, and a second touch alone did not show a bad design. The cap did blind
it inside a branch: all three verdicts on record that named it came after round
2. So a class round 1 fixed and round 2 found again in the same place went back
to the human.

The owner reported reading only the rows the agent flagged on governance diffs,
so "every row" had stopped describing what happened. Narrowing the chat made the
confirmation honest at a known cost: the agent chose what the human saw, which is
why the counts always appeared and an unrepeatable dismissal always went to the
human. Product branches kept every row, unmeasured until one tried this.

Issue #85 stayed open for what did not change rounds: an *accepted* outcome for
a cheap finding, the expiry of debt, and whether the label should split.

Only the thread resolution and the regression detector were policed; the rest
was prose.
