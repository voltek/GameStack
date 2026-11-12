---
name: build-feature
description: Start any feature that adds or changes what the user sees, however small, from the OpenSpec change first, then build it from Domain models to UI and close it, following Clean Architecture and MVI. Use before writing code for a new feature, screen or visible behaviour, whichever layer it starts in.
---

## Overview
This Skill orders the other GameStack Skills. Do not skip or reorder the steps: each
layer depends on the one before it. Keep the steps' progress visible to the owner, so
the checkpoint at step 5 is seen before it is reached.

Not exercised yet (#106): Search was built before OpenSpec and this order.
The first branch to run it corrects it and removes this note.

## Steps

### 0. Scope and the OpenSpec change
Check if the feature's scope is already unambiguous from its capability spec
in `openspec/specs/`, if one exists, the PRD
(`docs/project/PRODUCT.md`) and, if applicable, an existing
approved design reference (`docs/project/design/{screen-name}/`). If every
state and behaviour is written down, skip discovery. The PRD's section for a
capability whose *Spec* column says none yet does not reach that bar. If anything
is ambiguous, clarify it first with `discovery-feature`, or `openspec-explore` if
#206 adopts it instead.

Then open the feature's OpenSpec change with `openspec-propose` (ADR 0041). The
owner approves its proposal and delta spec before step 1, an ask-first decision like
step 5's. Its `tasks.md` turns steps 1–8 into tasks, one group per layer; build them
with `openspec-apply-change`, which ticks each task. In every step, each layer Skill
invokes `write-tests`, and a test a scenario names as its verifier is written before
its code.

### 1. Domain models
The pure domain model(s) the feature needs, beside the Repository that returns them:
`core/domain/model/` for a Repository in `core/`, which is the default
(`write-repository` § The interface), the feature's own `domain/model/` otherwise.

### 2. Repository
Invoke `write-repository`: the interface, then its implementation and its Hilt
binding in this same step (`write-repository` § The implementation), bringing in
`write-room-storage`, `write-mapper` and `write-igdb-endpoint` as it needs them.

### 3. UseCases
Invoke `write-usecase` for each thing the screen asks of the domain (`write-usecase`
§ Location and name).

### 4. Prototype
Generate a visual prototype (Stitch or Claude Design) from the approved change. If
`docs/project/design/{screen-name}/` already holds an approved export, ask the owner
whether it still applies instead of generating a new one.

### 5. Owner approval
Stop until the owner approves the prototype. A visual or UX choice is an ask-first
decision (CLAUDE.md § Working here): the agent proposes, the owner decides.

### 6. ViewModel
Invoke `write-viewmodel`, shaping `UiState` from the approved prototype and the spec's
states, with the UseCases from step 3.

### 7. Screen
Invoke `write-screen`, implementing the approved prototype's layout.

### 8. Navigation
Add the screen's type-safe route to `Destination` and its `composable<>` to the tab
graph that opens it, or to every tab graph when any tab can, as Detail is
(`.claude/rules/navigation.md`). Then wire the navigation call from where it is
launched, such as a game card navigating to Detail with the `gameId`. A screen not
wired into navigation is unreachable.

### 9. Close
- A feature that writes data also shows it, and one that shows data the user sets
  also writes it: rating a game saves the rating and shows the current one.
  `openspec-verify-change` checks only what the spec states, so a half the spec left
  out is caught here.
- `verify-on-device` for every screen the feature adds or alters, then
  `openspec-verify-change`, both before the reviews.
- A step this feature repeated that no Skill holds, or a Skill line the work proved
  wrong, is corrected in that Skill in this branch, and each `Not exercised yet` this
  feature ran is corrected and loses its marker (`.claude/rules/governance.md`).

## Quality criteria
- A step that does not fit the existing structure, such as a model that belongs
  clearly to neither `core` nor a feature, stops for the owner.
