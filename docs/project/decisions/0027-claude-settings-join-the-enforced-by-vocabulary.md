# ADR 0027 — Claude settings join the `Enforced by:` vocabulary

Status: Accepted
Date: 2026-10-23
Backfilled: no
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Documentation integrity — the value is in the closed vocabulary, and the rule a `claude settings` line names is in `.claude/settings.json`
Enforced by: none (prose only — decisions/README.md § `Enforced by:` — closed vocabulary) — only the committed file counts, never a local one
Enforced by: none (prose only — .claude/skills/ship-a-branch/SKILL.md § A governance diff earns the checklist) — a diff touching `.claude/settings.json` runs the checklist

## Context
The `Enforced by:` vocabulary had a value for every mechanism the repository
had used: prose, a checklist detector, lint, a test, the gate, branch protection,
and repository settings. None of them described a permission rule in Claude
Code's settings. Such a rule stops the agent before a command runs, which none
of the other values do. It binds only an agent session and not a person, and
argument patterns are documented as fragile. Rules of that kind already existed,
but only in a local `settings.local.json` that one machine's global ignore kept
out of git, so a clone did not get them and no ADR could point at them.

## Decision
Add `claude settings` to the closed vocabulary: a `deny` or `ask` rule in the
committed `.claude/settings.json`, named as `deny` or `ask` plus what it matches
in words. A line using it says `(the agent only, common spellings)`: the rule never binds a
person. A rule that exists only in a local settings file is not a mechanism and
is never cited.

A diff that touches `.claude/settings.json` is a governance diff and runs the
*Documentation integrity* detectors. Loosening a rule there unwrites an
`Enforced by:` line as surely as editing the ADR does.

## Consequences
The map could say what stops the agent in particular, separately from what stops
everyone. The cost was a value weaker than the others: a pattern matches one
spelling of a command, so the value records protection against a mistake and
never claims protection against an intent. Wherever a stronger mechanism covered
the same clause, it carried that clause for people too.
