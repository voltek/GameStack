---
name: write-screen
description: Create a Composable screen that observes a ViewModel following MVI, or change one. Use when asked to create a screen, UI or Composable for a feature, or to add a state, a preview or pull-to-refresh to one.
---

## Design reference
Read the approved export for this screen in `docs/project/design/{screen-name}/`
(HTML and PNG, both readable directly) and match its arrangement of elements and
spacing. It governs layout only: behaviour and content come from the capability's
spec in `openspec/specs/`, tokens from `docs/project/DESIGN.md`, and where the export
disagrees with either, the document wins (ADR 0042). Shared loading, empty and error
states are in `docs/project/design/states/`; their components already exist in
`core/presentation/components/`, so reuse them rather than drawing a new one.

## Construction
- Two Composables: `{Name}Screen`, stateful, takes the ViewModel and its navigation
  callbacks; and `internal {Name}Content`, stateless, takes `UiState` and
  `(UiEvent) -> Unit`. `internal` rather than `private`, so a screen test drives it
  without a ViewModel (`write-tests` § Screen tests).
- Collect state with `collectAsStateWithLifecycle()`: it stops collecting while the
  screen is not visible, which `collectAsState()` does not.
- Collect `UiEffect` in `LaunchedEffect(Unit)` in `{Name}Screen`. How effects are
  delivered is under evaluation in #175.
- Resolve every `UiText` from the state with `asString()`.
- Render every state the spec declares, each in its own branch. Search's are initial,
  loading, results, empty, error and refresh error.
- A list loaded from the network has pull-to-refresh, `PullToRefreshBox` bound to
  `isRefreshing`, sending an `OnRefresh` event that repeats the initial load's UseCase.
  The ViewModel sets `isRefreshing` in that event's handler, before any coroutine:
  the box starts hiding its indicator on release and only shows it again when it
  recomposes with the flag true. A list observed from Room has none, since it is
  already current (ADR 0070).

## Location and names
`feature/{name}/presentation/{ScreenName}Screen.kt`, holding `{ScreenName}Screen`,
`{ScreenName}Content` and their private parts.

## Previews
One `@Preview` per declared state, each wrapping `{Name}Content` in
`GameStackPreviewTheme`: in the app the `Scaffold` paints the background, so a
preview without it does not show the app's colours. Dark only, since the app has no
light theme (DESIGN.md).

## Visual consistency check
Compare the screen's code with the earlier screens' (colours, spacing, component
reuse), the reference files above and DESIGN.md's tokens. This is a check of code and
reference files; the device pass (`verify-on-device`) sees the pixels. A disagreement
between an export and a document is settled by ADR 0042. One between two screens is
not: name it, propose which is right, and wait for the owner.

## Quality criteria
- No logic beyond view logic in a Composable: rendering, event dispatch, and state
  that only the view needs (focus, a `remember`ed derivation).
- Every `UiEvent` the ViewModel defines is reachable from some interaction.
- Invoke `write-tests` for the screen tests: each declared state, and any
  Composable-level logic.
