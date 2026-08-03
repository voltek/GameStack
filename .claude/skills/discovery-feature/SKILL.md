---
name: discovery-feature
description: Asks a series of guided questions to turn a vague feature idea into a feature-scoped Spec draft, aware of existing project architecture. Use when adding a new feature to an existing project (not for starting a new project — see the `discovery` skill for that).
compatibility: Requires the target project to have a CLAUDE.md (or equivalent Constitution document).
---

## Questions
- What problem does this feature solve for the user, in one phrase?
- What's the minimum behavior for this feature to be useful on its own?
- What should this feature explicitly NOT do?
- Where does the user access this feature from? What existing screen/action triggers it?
- Does this feature touch or modify any existing screen, data model, or flow —
  or is it fully additive/isolated?
- Does this need new data, or does it reuse/extend data sources the project already has?

## How to ask
- Iterate — go deeper if an answer is vague or ambiguous.
- Don't ask all questions at once.
- Prefer closed/multiple-choice questions when the ambiguity is concrete;
  open questions when genuine exploration is needed.
- If the agent has a native structured-question tool available (e.g., Claude
  Code's AskUserQuestion), prefer it for closed/multiple-choice questions.
  Otherwise, ask directly in the conversation — this is a preference, not a
  hard requirement, so the skill works on any tool.
- Cross-check answers against the project's CLAUDE.md — if something conflicts
  with an existing Tier 1 rule, surface that conflict explicitly before proceeding.

## Beyond the list
The questions above are the required minimum — never skip them. But they are
a floor, not a ceiling. If something about this specific feature surfaces a real
ambiguity or risk not covered by the list, ask about it too. Note explicitly
why you're asking beyond the standard list, so it's clear this is a deliberate
addition, not an inconsistent process.

## When to stop
- Stop once there's enough clarity to move directly to Work Decomposition
  (invoking `new-feature`). Unlike project-level discovery, there is no
  Technical Blueprinting phase here — the project's architecture already exists.

## Result
A feature-scoped Spec draft with this structure:
- What it is (one phrase)
- Minimum behavior
- What it is NOT
- Entry point (where it lives in the existing app)
- Touches existing systems? (yes/no + what)
- Data: new or reused
- Pending / Open Questions

Append this as a new section to the project's master Spec file — do not
create a separate spec file per feature. One source of truth avoids the
same drift risk that multiple scattered documents would create.

## Quality criteria
- The "touches existing systems" answer is explicit — never left ambiguous,
  since this determines blast radius.
- No unresolved ambiguity remains — anything undecided goes into
  "Pending / Open Questions", not silently assumed.
