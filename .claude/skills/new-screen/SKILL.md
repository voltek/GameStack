---
name: new-screen
description: Create a Composable screen that observes a ViewModel following MVI. Use when asked to create a screen, UI, or Composable for a feature, or as part of the new-feature skill.
---

## Design reference
Before building, check `docs/project/design/{screen-name}/` for the approved
Stitch export (HTML + screenshot) of this screen. Match its layout and
spacing exactly — the DESIGN.md governs colors/typography/tokens, but the
approved export governs the specific arrangement of elements for this screen.

For reusable state components (loading skeleton, empty state, error state),
check `docs/project/design/states/` instead — these are shared across screens,
not specific to one.

## Construction
- Collect state with `collectAsStateWithLifecycle()`, never `collectAsState()`.
- Handle `UiEffect` inside `LaunchedEffect(Unit)`, collecting from the ViewModel's effect Flow.
- Structure: a stateless `{Name}Content` Composable that receives `UiState` and
  `(UiEvent) -> Unit`, plus a stateful `{Name}Screen` that wires the ViewModel.
  This keeps the Content Composable previewable without a real ViewModel.
- Use `UiText.asString()` to resolve any text coming from the state/effects.
- Cover loading, error, and content states explicitly — never leave a state unhandled.
- If the screen displays a list/collection of items (e.g. Home sections, Search
  results, Library lists) — NOT a single-item screen like Detail — implement
  pull-to-refresh:
  - Add `isRefreshing: Boolean` to `UiState`.
  - Add a `Refresh` case to `UiEvent`, triggering the same UseCase used for
    the initial load (no separate "refresh" UseCase needed).
  - Wrap the list content in Compose's pull-to-refresh composable, bound to
    `isRefreshing`.

## Visual consistency check
This is a code-level and reference-file check — not a live rendering
comparison (that capability comes later, via Android CLI's Compose Preview
rendering). Compare against:
- Previously written screen source files (colors, spacing, component reuse)
- The saved reference files in `docs/project/design/{screen-name}/` and
  `docs/project/design/states/` (PNG/HTML — can be read directly as images)
- DESIGN.md's tokens

If a discrepancy is found — either between screens, or between a screen and
DESIGN.md — do not silently pick one to follow. Surface the discrepancy
explicitly, propose which should be treated as correct, and wait for human
approval before proceeding.

## Location
`feature/{name}/presentation/{ScreenName}Screen.kt`

## Naming convention
`{ScreenName}Screen` (stateful, wires ViewModel), `{ScreenName}Content` (stateless, previewable)

## Quality criteria
- Must include a `@Preview` — Dark theme only (MVP has no Light theme, see CLAUDE.md).
- No business logic in the Composable — only rendering and event dispatch.
- Every `UiEvent` the ViewModel defines must be reachable from some UI interaction.
- After creating the Screen, invoke the `write-tests` skill if any non-trivial
  state-mapping logic was added (pure rendering typically doesn't need unit tests).
