# ADR 0050 — Rules say what an agent does, and each line in CLAUDE.md earns its place

Status: Accepted
Date: 2026-11-05
Backfilled: no
Supersedes: ADR-0006 — reading `git status` in full, and the section being tiered (Decision)
Supersedes: ADR-0039 — "as a Tier 1 rule" (Decision)
Enforced by: none (prose only — CLAUDE.md § Working here) — asking first, how a rule changes
Enforced by: none (prose only — .claude/rules/governance.md § What earns a line in CLAUDE.md) — the admission questions, a line with no effect leaving, the warning before a large batch of runs
Enforced by: none (prose only — .claude/skills/ship-a-branch/SKILL.md § A governance diff earns the checklist) — `.claude/rules/` in the shape

## Context
CLAUDE.md sorted rules into three tiers by how hard each was to change. No source
consulted in November 2026 classified rules that way: Anthropic's documentation
had no severity levels and called CLAUDE.md context rather than enforced
configuration, and the three levels GitHub and Addy Osmani described sorted actions
by the agent's autonomy. The label enforced nothing: 73 of 120 `Enforced by:` lines
read "none". The autonomy axis existed anyway, split between Human-in-the-Loop and
the `ask` rules.

Ablation on 2026-11-05 replaced opinion with evidence for most lines: a fresh agent
got the same task with and without one line, on `claude-sonnet-5-5`, two runs each.
The evidence was thin, and the batch ended at the owner's usage limit.

## Decision
Tiers leave. CLAUDE.md § Working here states what an agent always does, what it
asks first and what it never does. Elsewhere a rule uses plain wording, and a
"Prefer" heading marks rules that may be challenged with a stated reason. A rule
changes only with the owner's agreement, through an ADR.

A line stays in CLAUDE.md only if removing it changes what an agent does. What must
hold every time is a hook, a `deny` rule or a test. A pattern matches one spelling of
a command, so a rule it only partly imposes keeps its line.

Rules for writing governing documents load from `.claude/rules/governance.md`, by
path, when one is touched. `.claude/rules/` joins the governance diff's shape.

The owner is warned before a large batch of runs, which spends their usage limit.
Ordinary subagent use needs no warning.

## Consequences
Several lines stayed on judgement after their comparison was lost to the limit:
rebasing a pushed branch, committing on `main`, the rule of three, Purpose and how a
rule changes. Reading `git status` in full left because it showed no effect, and
committing as you go, with housekeeping, because its measure was inconclusive and it
was only a suggestion.

The first draft dropped every rule `deny` or `ask` covered. Review showed the
patterns missed spellings, so `--no-verify`, force-pushing, PR actions and repository
settings kept their lines. Deleting a remote branch kept only its `ask` rule: unusual
spellings passed the pattern, and GitHub deleted a merged branch by itself.

"The trade this makes" left by the owner's decision, with its test for restoring a
moved story. ADRs written before this one kept saying "Tier", frozen as they were.
