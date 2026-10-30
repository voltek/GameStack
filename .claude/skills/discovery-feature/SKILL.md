---
name: discovery-feature
description: Asks a series of guided questions to turn a vague feature idea into a feature-scoped spec draft for its OpenSpec change, aware of existing project architecture. Use when adding a feature to a project that already has a PRD and a CLAUDE.md — not for defining a brand-new project from zero.
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
- Stop once there's enough clarity to move directly to implementation
  (invoking `new-feature`). There is no architecture/blueprinting phase here —
  the project's stack and layering already exist and are governed by CLAUDE.md.
  This skill decides *what* the feature is, never *how* it's built.
- A capability with a spec in `openspec/specs/` is settled by it; reopen
  nothing it answers. One whose *Spec* column in the PRD says none yet is not
  settled by its PRD section and mockups: check both, and turn every gap into
  minimum behavior or an open question.

## Result
A feature-scoped spec draft with this structure:
- What it is (one phrase)
- Minimum behavior
- What it is NOT
- Entry point (where it lives in the existing app)
- Touches existing systems? (yes/no + what)
- Data: new or reused
- Pending / Open Questions

Behaviour a user sees is never appended to the PRD: it goes into an
OpenSpec change for the feature's branch (ADR 0041) — minimum behavior as
requirements and scenarios, open questions under the proposal's
`## Open Questions`. In the pilot on Detail's spec, `openspec-propose` writes
the change and these answers feed it; whether this Skill survives is the
pilot's verdict (TOOLING.md § 13, **Pilot.**).

## Quality criteria
- The "touches existing systems" answer is explicit — never left ambiguous,
  since this determines blast radius.
- No unresolved ambiguity remains — anything undecided goes into
  "Pending / Open Questions", not silently assumed.
