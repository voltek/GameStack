---
name: new-feature
description: Create a complete GameStack feature end-to-end, from Domain models to UI, following Clean Architecture and MVI. Use when asked to build a new feature or screen from scratch.
---

## Overview
This skill orchestrates the other GameStack skills in the correct dependency order.
Do not skip steps or reorder them — each layer depends on the previous one.

## Steps

### 1. Domain models
Create the pure domain model(s) needed for this feature.
Location: `core/domain/model/` if shared, `feature/{name}/domain/model/` if feature-specific.

### 2. Repository interface
Invoke the `new-repository-interface` skill. Make sure both read and write functions
are included if the feature needs both.

### 3. Repository implementation
Invoke the `new-repository-impl` skill. This step internally creates the required
DTOs/Entities, invokes `new-mapper` for any Entity/DTO → Domain conversion, and uses
the `new-api-service` skill for any new IGDB endpoint.

### 4. UseCases
Invoke the `new-usecase` skill once for each Repository function the feature needs to expose.

### 5. ViewModel
Invoke the `new-viewmodel` skill, injecting the UseCase(s) created in step 4.

### 6. Screen
Invoke the `new-screen` skill, wiring the ViewModel created in step 5.

### 7. Tests
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
