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

## Behaviour
Behaviour lives in `openspec/specs/`, or in the PRD for a capability with no spec yet.
A `feat` branch changing what the user sees starts from an OpenSpec change (ADR 0041).

## Device verification
The gate proves that the code compiles and that the logic does what a test asserts,
never that a screen looks right or that a control is reachable. So every change that
adds or alters a screen is verified on the emulator before a merge is requested,
covering every state the screen declares, and the PR body says what was checked.
Data or domain work with no UI change does not need this pass. Run the
`verify-on-device` Skill for the procedure.

Any element or state that departs from what the approved exports show is added to
`docs/project/design/` as both `.html` and `.png`, in that screen's folder, in the
same change that ships it. Recipe and naming: `docs/project/design/README.md`.

## Accessibility
The MVP does not target accessibility. TalkBack passes, large-font and contrast
audits and touch-target sizing are deferred until the first release (PRD), so an
accessibility finding from a review is deferred by default (`ship-a-branch`, Triage).
Do not build accessibility work that was not asked for, and do not hold a PR for it.

Two things this does not license, because both cost far more to retrofit than to keep:
- Dropping an affordance that already worked. Material components carry semantics for
  free: a `Snackbar` announces itself, an `IconButton` exposes a focus target. A
  hand-built replacement silently loses that, invisibly to the gate and to a
  screenshot. If a change removes an announcement or a focus target, restore it in
  that same change: that is not new accessibility work, it is not regressing.
- Letting the deferral outlive its expiry. The app is headed for the Play Store, which
  publishes accessibility expectations and where retrofitting semantics across a
  finished UI is a rewrite. Revisit before the first public release, not after.
