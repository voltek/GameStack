---
name: discovery-feature
description: Asks a series of guided questions to turn a vague feature idea into the answers its OpenSpec change needs, aware of existing project architecture. Use when adding a feature to a project that already has a PRD and a CLAUDE.md — not for defining a brand-new project from zero.
compatibility: Requires the target project to have a CLAUDE.md.
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
- Ask a closed question with the agent's structured-question tool where it has one
  (Claude Code's AskUserQuestion), otherwise in the conversation.
- Cross-check answers against the project's CLAUDE.md and `.claude/rules/` — if
  something conflicts with an existing rule, surface that conflict explicitly before proceeding.

## Beyond the list
The questions above are a floor, not a ceiling: ask each, and ask about any other
ambiguity or risk this feature raises, saying what prompted it.

## When to stop
- Stop once the answers can fill the feature's OpenSpec change (`openspec-propose`,
  `new-feature` step 0). This Skill decides what the feature is, never how it is
  built: the stack and layering are CLAUDE.md's.
- A capability with a spec in `openspec/specs/` is settled by it; reopen
  nothing it answers. One whose *Spec* column in the PRD says none yet is not
  settled by its PRD section and mockups: check both, and turn every gap into
  minimum behavior or an open question.

## Result
The answers, for `openspec-propose`:
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
`## Open Questions`.

Not exercised yet (#106): no feature has run it with OpenSpec.
The first branch to run it corrects it and removes this note.

## Quality criteria
- The "touches existing systems" answer is explicit — never left ambiguous,
  since this determines blast radius.
- No unresolved ambiguity remains — anything undecided goes into
  "Pending / Open Questions", not silently assumed.
