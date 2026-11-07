---
paths:
  - "app/src/main/**/presentation/**"
  - "app/src/main/**/navigation/**"
  - "app/src/main/**/ui/**"
  - "app/src/main/res/**"
  - "app/src/main/AndroidManifest.xml"
  - "app/src/main/java/com/gamestack/MainActivity.kt"
  - "docs/project/design/**"
  - "openspec/changes/**"
---

# Changing what the user sees
A `feat` branch changing what the user sees starts from an OpenSpec change (ADR 0041).

## One-shot events
A one-shot event, such as a navigation or a snackbar, is a `UiEffect`, never `UiState`,
which replays on recomposition. A condition that lasts, such as stale results, is state
(ADR 0017). Effects travel on a `Channel` that queues while the screen is away, so a
duplicate fires again when the user returns. One user intent therefore emits exactly
one effect: wrap every callback that emits a navigation effect in `rememberSingleClick`,
sharing one wrapper across a list, and audit this per screen, not per list (ADR 0011,
under evaluation in #175).

## Device verification
The gate cannot show that a screen looks right or that a control is reachable. So every
change that adds or alters a screen is verified on the emulator before a merge is
requested, covering every state the screen declares, and the PR body says what was
checked. The procedure is the `verify-on-device` Skill.

Any element or state that departs from what the approved exports show is added to
`docs/project/design/` as both `.html` and `.png`, in that screen's folder, in the
same change that ships it. Recipe and naming: `docs/project/design/README.md`.

## Accessibility
The MVP does not target accessibility (ADR 0013): do not build it unasked and do not
hold a PR for it. A review finding on it is deferred by default.

That does not license a regression. A `Snackbar` announces itself and an `IconButton`
exposes a focus target, and a hand-built replacement loses both, invisibly to the gate
and to a screenshot. If a change removes one, restore it in that same change.
