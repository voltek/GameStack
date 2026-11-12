---
paths:
  - "CLAUDE.md"
  - ".claude/rules/**"
  - ".claude/skills/**"
  - "docs/project/**"
  - ".github/**/*.md"
  - ".claude/settings.json"
  - "openspec/schemas/**"
---

# Writing the documents that govern this repository

## What earns a line in CLAUDE.md
CLAUDE.md loads on every request, and adherence falls as it grows. It holds at most
60 lines and 3,600 characters, which a test checks (ADR 0055).

Before adding a line here or to a `.claude/rules/` file, ask in order. Moving a line
is no reason to keep it, and the questions apply to what is already there (ADR 0053):
1. **Could an agent find this by reading the repository, and would it go and look?**
   If both, leave it out.
2. **Does it apply whatever the session touches?** If it binds one part of the code,
   it goes in a `.claude/rules/` file with `paths:`. If it is a sequence followed
   during one task, it goes in that task's Skill.
3. **Must it hold every time?** Then it is a hook, a `deny` rule or a test, and where
   the check has a message, it carries the reason. A pattern matches one spelling of a
   command, so a rule it only partly imposes keeps its line. A CLAUDE.md line is a
   guideline.
4. **Does it survive a clone?** A path on one machine or a device serial belongs in
   agent memory.
5. **Does removing it change what an agent does?** A line with no effect leaves.
6. **Does the source support it?** Read the platform's guidance rather than recalling
   it. A rule that departs from that guidance on purpose says so and why.

## Generated Skills
The `openspec-*` Skills are generated and `openspec update` rewrites them. A `deny`
covers their folder but misses other spellings, so never edit one by any route:
project specifics go to the schema and the config (ADR 0041).

## How to word a rule
Keep the reason, not the story: the consequence that makes a rule necessary stays
beside it in the present tense, and who and when go to the commit body. Use plain
wording and no emphasis: emphasis makes a rule over-apply
without making it stronger.

## Completeness
A reference to a component that does not fully exist is specified in the same edit,
or marked `(PENDING — not yet defined)`. A vague reference that looks complete hides
the gap until an agent trips over it mid-task.

## Not exercised yet
A Skill section that no product code has run yet carries `Not exercised yet (#n):`,
what does not exist, and "The first branch to run it corrects it and removes this
note.", #n being the issue of the feature that runs it first. It tells the agent the
section is unproven, so a step the code contradicts is corrected, not forced. The
marker carries its own instruction because a Skill run through the Skill tool may
never load this file, and a section can run after `new-feature` step 9, as a
review or a merge does. It is not a story about the future: it holds until a branch
runs the section, which corrects it and removes the note before its merge (ADR 0073).
