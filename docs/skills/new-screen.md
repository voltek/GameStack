# new-screen Skill

Source: `.claude/skills/new-screen/SKILL.md`

## Purpose
Creates a Composable screen that observes a ViewModel, following MVI conventions.

## Key design decisions
- Split into a stateless `{Name}Content` (receives UiState + event lambda, previewable) and a stateful `{Name}Screen` (wires the real ViewModel).
- Uses `collectAsStateWithLifecycle()`, never `collectAsState()`.
- `UiEffect` handled inside `LaunchedEffect(Unit)`.
- Must include `@Preview` for both Light and Dark theme.

## Iteration history
- v1: initial version