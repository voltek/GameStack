# ADR 0070 — Pull-to-refresh is for a list loaded from the network

Status: Accepted
Date: 2026-11-11
Backfilled: no
Enforced by: none (prose only — .claude/skills/new-screen/SKILL.md § Construction) — pull-to-refresh on a list loaded from the network, none on a list observed from Room

## Context
The rule had come from CLAUDE.md to `new-screen` (ADR 0053) and gave every list
screen pull-to-refresh, naming Home's sections, Search's results and Library's
lists. Search's results came from IGDB, and Home's sections were to as well. Library's
lists were the user's own and lived only on the device (PRODUCT.md: "no account and
no server of ours"), so nothing outside the app could change them. They were to be
read from Room as a `Flow`, which emits again on every change to the rows it reads.
A pull on Library would have repeated a read the `Flow` already kept current.

## Decision
- A list loaded from the network has pull-to-refresh, repeating its initial load's
  UseCase.
- A list observed from Room has none: it is already current.

## Consequences
Library lost a gesture that would have done nothing. A list whose data later gains
a source outside the device gains pull-to-refresh by the same rule, with no new
decision. No screen yet mixes both kinds of list, so what one would refresh was left
undecided until one exists.
