# new-repository-interface Skill

Source: `.claude/skills/new-repository-interface/SKILL.md`

## Purpose
Defines the Repository contract in the Domain layer — interface only, no implementation. Functions use only Domain models, never DTOs/Entities/Android types, and are wrapped in `Result<T>` when they can fail.

## Key design decisions
- Both read and write functions are included together when a feature needs both — no artificial split into read-only/write-only interfaces.
- Location depends on scope: `core/domain/repository/` if shared, `feature/{name}/domain/repository/` if feature-specific.

## Iteration history
- v1: initial version