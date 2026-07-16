# new-usecase Skill

Source: `.claude/skills/new-usecase/SKILL.md`

## Purpose
Creates a UseCase that invokes a single Repository function, using `operator fun invoke()`.

## Key design decisions
- Returns `Flow<Result<T>>` for observable data, or `suspend operator fun invoke()` for one-shot writes.
- Errors from the Repository are always propagated, never swallowed or transformed silently.
- Starts feature-specific by default (`feature/{name}/domain/usecase/`); moves to `core/domain/usecase/` only once a second feature actually needs it.

## Iteration history
- v1: initial version