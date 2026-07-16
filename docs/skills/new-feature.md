# new-feature Skill

Source: `.claude/skills/new-feature/SKILL.md`

## Purpose
Orchestrator Skill — builds a complete feature end-to-end (Domain → Data → Presentation → UI), invoking the other 8 Skills in strict dependency order.

## Key design decisions
- Steps cannot be skipped or reordered — each layer depends on the previous one.
- `write-tests` is invoked automatically by each individual Skill as it completes, not batched at the end.
- If a model doesn't clearly belong to `core` or a specific `feature`, the agent must stop and ask before proceeding, rather than guessing.

## Iteration history
- v1: initial version