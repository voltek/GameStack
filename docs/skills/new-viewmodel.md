# new-viewmodel Skill

Source: `.claude/skills/new-viewmodel/SKILL.md`

## Purpose
Creates a ViewModel following MVI, defining the three contracts: `UiState`, `UiEvent`, `UiEffect`.

## Key design decisions
- `UiState` as data class by default; sealed class only when states are truly mutually exclusive.
- `UiEffect` exposed via `Channel`/`receiveAsFlow()`, not SharedFlow (see Block 2 rationale).
- Single public entry point `handleEvent(event: UiEvent)`, routing internally via `when`.
- `UiText` used for all exposed text, never raw strings.

## Iteration history
- v1: initial version