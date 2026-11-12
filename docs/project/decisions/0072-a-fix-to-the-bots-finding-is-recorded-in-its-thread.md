# ADR 0072 — A fix to the bot's finding is recorded in its thread

Status: Accepted
Date: 2026-11-12
Backfilled: no
Supersedes: ADR-0063 — "followed by one list item per fix no round read" (Decision), for a fix to the GitHub bot's finding
Enforced by: branch protection — `required_conversation_resolution=true` (the bot's thread resolved before the merge; the reply saying what was done is prose)
Enforced by: none (prose only — .github/pull_request_template.md § Review) — no line in *Review* for a fix to the bot's finding

## Context
ADR 0063 listed in a PR body's *Review* every fix no round read, and a fix to the
GitHub bot's finding was one, since the bot runs one round per PR. Each such fix
was already answered in the bot's own thread, which says what was done next to
the line the finding named and stays on the pull request after the merge; the
thread had to be answered and resolved before the merge could happen. The body
line repeated that reply, and adding it cost an edit of the body, which reran
`commits.yml` on the same commit. The owner judged the line added little (#94).

## Decision
A fix to the GitHub bot's finding is recorded in the reply on its thread, which
says what was done, and takes no line in the PR body's *Review*. That list holds
the fixes made after the last `/code-review` round.

## Consequences
A bot finding fixed left the PR body as it was, so the body was edited only when
the branch's claim changed. Whoever read the body alone no longer saw that the
branch carried a change no reviewer had read; the threads, resolved and folded
by GitHub's interface, were where to look. The first line's count of rounds kept
counting `/code-review` rounds only, as before.
