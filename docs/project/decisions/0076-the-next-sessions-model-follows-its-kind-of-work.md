# ADR 0076 — The next session's model follows its kind of work

Status: Accepted
Date: 2026-11-12
Backfilled: no
Enforced by: none (prose only — .claude/skills/checkpoint/SKILL.md § The next session's model) — the model and effort named for the next session, and the table checked once

## Context
Every session ran on whatever model and effort the last one had used, and nothing
said which fit the work ahead (#194). #194 proposed a table of recommended models
by kind of session, and a script that logged each session's tokens beside the
handoff for every triage to tune the table. Measured (#194's comment of
2026-11-12), the sessions of one week, all on Opus at `high`, read between 1.6 and
76 million cached tokens each, a spread set by the task, and a cycle ran one session
per task across a dozen kinds of session.

## Decision
The checkpoint's closing report names the model and effort for the next action,
read from a table of kinds of session in the `checkpoint` Skill, never weighed task
by task. The table names model families, not versions. The owner switches at the
next session's start.

The table is checked once, at one triage, against the sessions the owner saw go
wrong (#214), and stays as it is after. Each session's tokens are not logged.

## Consequences
The next session started from a recommendation that cost one line of the closing
report and a table read only at a checkpoint. The table stayed a hypothesis from
Anthropic's guidance, checked once by what the owner saw rather than by a measure,
and no triage carried it as a recurring task. A per-session log was ruled out: at
one person's volume it could not separate the model's effect from the task's for
many cycles, and its script, tests and step cost more than the recommendation it
would refine. Dollars, derived from tokens and a price that changes, were kept
nowhere.
