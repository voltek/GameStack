# ADR 0017 — A condition is not an event; `UiState` is a data class

Status: Accepted
Date: 2026-08-23
Backfilled: yes — DRIFT-LOG.md, rows 2026-08-23 ("`SearchScreen` evaluated `errorMessage != null`") and ("Third `/code-review` round on PR #5")
Enforced by: none (prose only — CLAUDE.md § Architecture)

## Context
`SearchScreen` evaluated `errorMessage != null` as its first branch, so a failed
pull-to-refresh replaced a full grid of loaded results with an error card. The
state modelled the situation correctly; the UI rendered its fields as if they
were mutually exclusive.

The first fix announced the failure with a snackbar, and that mechanism then
drifted from what it described three separate times: a later successful refresh
left the failure message over fresh results, an effect queued while the screen
was disposed replayed on return, and each guard closed one path and left the
others. Third touch of the same logic for the same class of defect.

## Decision
`{ScreenName}UiState` is a **data class**, exposed as `StateFlow`. A sealed class
is used only when the states are genuinely mutually exclusive — and loading,
content and error frequently coexist, a refresh failing while stale content is
still on screen being the standard case.

The distinction that resolves the mechanism: a **persistent condition** ("these
results are stale") is state, and a **transient event** (a navigation, a
one-shot announcement) is an effect. Announcing a condition with a transient
mechanism means their lifetimes can only drift apart. `refreshError` therefore
lives in `SearchUiState` and drives an inline banner the ViewModel clears on a
successful load or a query change.

This does not weaken the rule against one-shot events in `UiState`: that rule
exists because an event modelled as state fires twice, and a condition is not an
event.

## Consequences
The snackbar, its `Channel` round trip, its query guard and its dismissal logic
all disappeared together — the redesign deleted more than it added, which is the
usual sign the third patch was the wrong move.

A failed refresh keeps the results it failed to replace, while a failed search
for *new* text still shows the error state, because the results on screen answer
the previous query and keeping them would misrepresent what was searched.

The cost: "condition or event" is a judgement made per field, and it is the
judgement the checklist cannot make. The nearest detector guards the opposite
direction — a one-shot event modelled in `UiState` — so the failure this ADR
actually fixed has nothing mechanical behind it.
