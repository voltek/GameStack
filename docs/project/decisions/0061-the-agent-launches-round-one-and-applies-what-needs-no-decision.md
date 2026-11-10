# ADR 0061 — The agent launches round one and applies what needs no decision

Status: Accepted
Date: 2026-11-10
Backfilled: no
Supersedes: ADR-0049 — "`/code-review` is run by the human" (Decision)
Supersedes: ADR-0058 — "nothing is committed, folded, reviewed again or opened as a PR before every row and the verdict are confirmed" (Decision), for round 1's fixes that need no decision
Enforced by: commits.yml — the extractor's test: the shapes it saves and the ones it refuses loudly
Enforced by: none (prose only — .claude/skills/review-round/SKILL.md § Before each round, ask) — the agent launching on the yes, the level proposed with its reason
Enforced by: none (prose only — .claude/skills/review-round/SKILL.md § The sequence) — round 1's fixes applied alone, one commit each, the rest held
Enforced by: none (prose only — .claude/skills/review-round/SKILL.md § The message) — what the human decides, a chosen fix reported with the one dropped, round 1's counts, round 2 asked for after the decisions are folded
Enforced by: none (prose only — .claude/skills/review-round/references/extractor-failed.md § When `save-review.mjs` fails) — the record copied from the notification saying so

## Context
Every `/code-review` round waited for the owner to launch it, although the agent
could invoke the Skill, as it did in PR #166's round 2, and the review subagent
started with no cache read from its parent, so it did not carry the author's
context. Round 1's triage then waited for the owner even when every row had one
obvious fix, and the owner reported taking the agent's recommendation on nearly
every governance fix with a choice in it. The raw record was retyped from the subagent's notification, about
2,000 output tokens a round, and the notification escaped `<` and `>`. A pilot
and three measured runs showed `claude -p` could choose a cheaper model at no
visible cost of its own, but on one governance diff no cheaper model found the
three defects, and Opus repeated only two (#187).

## Decision
The agent launches `/code-review` through the Skill, on the session's model and
at a level it proposes with a one-line reason, after the human's explicit yes.

In round 1, apply before the message every fix the human need not decide, a code
defect only with its regression test seen to fail, and commit each as its own
fold commit, so the human can drop one. The human decides a fix that writes a
rule or changes what one requires of an agent with no document or code pattern
settling it, one the user would see, and one outside the branch's unit of work.
Among several reasonable fixes, choose one and report it with the one dropped, a
few words each. This narrows CLAUDE.md's ask-first for a trade-off with no single
answer, for round 1's fixes only.

Hold every other row, and open no issue, until the human answers. With a row for
the human to decide or one leaving the branch, ask for round 2 only once the
decisions are applied and folded. Later rounds apply nothing before the human
confirms.

Save each round's raw text from the subagent's transcript with the Skill's
script. A shape it does not expect fails loudly, never with an empty record; a
record copied from the notification says so on its first line. Record the
reviewer's model and level with the table.

## Consequences
The owner stopped launching rounds and stopped confirming fixes the documents
settled or that held a small choice, and kept every decision that changed a rule,
what the user saw or the branch's scope. The agent's choice now stood unless
reopened, so a bias in it passed more easily to an owner who mostly agreed: the
dropped option in the message was the counterweight, and how often the owner
reversed one on product code was left to #187's first product branch. A fix
applied alone stayed unseen until the checkpoint: its own commit made it cheap to
drop, and round 2 read every one. The checkpoint cost a message even with nothing
to decide. Then one bare yes launched round 2 over fixes the owner knew only as
counts, a cost the owner chose; with something to decide, round 2 took its own yes
once the decisions were folded.

The extractor rested on an undocumented transcript format that a new version
could change. The loud failure and the fallback to the notification bounded
that, at the cost of an escaped record until a repair branch. Cheaper models and
the `claude -p` launcher waited in #187.

Three options lost. Stopping before round 2 only when a row needed a decision
let a dismissal the owner turned into a fix land where no round read it. An
`Auto-fixes` count and a shadow trial measured what round 2's `Origin` column
already recorded. Asking for round 2 in every checkpoint message let one yes
cover decisions the owner was still making.
