---
name: new-feature
description: Create a complete GameStack feature end-to-end, from Domain models to UI, following Clean Architecture and MVI. Use when asked to build a new feature or screen from scratch.
---

## Overview
This skill orchestrates the other GameStack skills in the correct dependency order.
Do not skip steps or reorder them — each layer depends on the previous one.

If the agent has a native task-tracking tool available (e.g., Claude Code's
TodoWrite/TaskCreate, depending on version), use it to track progress through
the 10 steps below (0–9) — this makes the pending human checkpoint (step 5)
visible. Otherwise, list progress in plain text at each step.

## Steps

### 0. Confirm scope is clear (conditional)
Check if the feature's scope is already unambiguous from the product Spec
(`docs/project/GameStack-Spec-v1.md`) and, if applicable, an existing
approved design reference (`docs/project/design/{screen-name}/`). If yes
— e.g. Home, Search, Library, Detail are already fully specced and
designed — skip directly to step 1. If genuine ambiguity remains (a newer,
less-defined feature idea), invoke the `discovery-feature` skill first.

### 1. Domain models
Create the pure domain model(s) needed for this feature.
Location: `core/domain/model/` if shared, `feature/{name}/domain/model/` if feature-specific.

### 2. Repository interface
Invoke the `new-repository-interface` skill. Make sure both read and write functions
are included if the feature needs both.

### 3. Repository implementation
Invoke the `new-repository-impl` skill. This step internally:
- creates the required DTOs, and invokes `new-room-dao` for any local storage
  (Entity, DAO, TypeConverters, Database registration);
- invokes `new-mapper` for each Data/Domain conversion — `toDomain()` on the read
  path, `toEntity()` on the write path;
- invokes `new-api-service` for any new IGDB endpoint;
- invokes `new-hilt-module` to bind the Impl to its interface and provide anything
  new it depends on. A Repository that isn't bound compiles nowhere — do not defer this.

### 4. UseCases
Invoke the `new-usecase` skill once for each Repository function the feature needs to expose.

### 5. Screen prototype
Generate a visual prototype (Stitch or Claude Design) based on the
Spec/discovery-feature output.

STOP HERE. Do not proceed to ViewModel/Screen until the human explicitly
approves the prototype. Visual/UX judgment is not mechanical — this is a
Plan-level decision (CLAUDE.md → "Human-in-the-Loop principle"), not an
Implement-level one. The agent proposes, the human decides.

Note: the four MVP screens (Home, Search, Library, Detail) already have approved
exports in `docs/project/design/`, and the shared loading/empty/error states in
`docs/project/design/states/`. If this feature is one of those, the approval
already happened — confirm the existing design still applies rather than
generating a new prototype (see the paragraph below).

Note for the human: consider running this entire skill in Claude Code's
Plan Mode — it enforces this pause mechanically (read-only until you
approve), rather than relying solely on the agent following this instruction.

If a project-wide design pass already exists and covers this screen, confirm
with the human whether that existing design still applies, rather than
silently reusing or silently regenerating a new one.

### 6. ViewModel
Invoke the `new-viewmodel` skill, using the approved prototype's fields to
shape UiState accurately, injecting the UseCase(s) created in step 4.

### 7. Screen
Invoke the `new-screen` skill, implementing the approved prototype's layout.

### 8. Navigation wiring
Register the new screen in the root navigation graph (built by
`project-scaffold`, if one exists): add its type-safe route, and wire up
the navigation call from wherever it's launched (e.g. tapping a game card
navigates to Detail with the gameId as a typed argument). A screen built
but never wired into navigation is unreachable — do not skip this step.

### 9. Tests
`write-tests` is invoked automatically by each skill above as it completes.
Do not skip or batch this — each skill is responsible for its own test coverage.

## Quality criteria
- Every step above must be completed before moving to the next.
- If a step doesn't fit the existing structure (e.g. a model doesn't clearly belong
  to `core` or a `feature`), stop and ask before proceeding.
- No layer imports from a layer above it — see CLAUDE.md rules.
- Before finalizing, confirm the feature supports both reading and writing data
  if the feature's purpose requires both (e.g. rating a game requires saving AND
  displaying the current rating).
