---
name: new-screen
description: Create a Composable screen that observes a ViewModel following MVI. Use when asked to create a screen, UI, or Composable for a feature, or as part of the new-feature skill.
---

## Construction
- Collect state with `collectAsStateWithLifecycle()`, never `collectAsState()`.
- Handle `UiEffect` inside `LaunchedEffect(Unit)`, collecting from the ViewModel's effect Flow.
- Structure: a stateless `{Name}Content` Composable that receives `UiState` and
  `(UiEvent) -> Unit`, plus a stateful `{Name}Screen` that wires the ViewModel.
  This keeps the Content Composable previewable without a real ViewModel.
- Use `UiText.asString()` to resolve any text coming from the state/effects.
- Cover loading, error, and content states explicitly — never leave a state unhandled.

## Location
`feature/{name}/presentation/{ScreenName}Screen.kt`

## Naming convention
`{ScreenName}Screen` (stateful, wires ViewModel), `{ScreenName}Content` (stateless, previewable)

## Quality criteria
- Must include a `@Preview` for both Light and Dark theme.
- No business logic in the Composable — only rendering and event dispatch.
- Every `UiEvent` the ViewModel defines must be reachable from some UI interaction.
- After creating the Screen, invoke the `write-tests` skill if any non-trivial
  state-mapping logic was added (pure rendering typically doesn't need unit tests).
