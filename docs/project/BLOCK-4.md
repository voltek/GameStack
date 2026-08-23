# Block 4 — Loop Engineering

Harness work, not product: automating what is currently a manual review pass.
The Spec's Roadmap Phases section names this block in one line and points here; the
design notes live here because they are notes about work not yet done, not rules
constraining work being done now.

Three deliverables, in priority order.

## 1. Automate the regression-test verification
**Ranked first.** CLAUDE.md → Testing Stack (Tier 1) requires every regression
test to be *seen* to fail against the code it guards, today via a manual
`git stash` recipe (or a targeted mutation when the fix is a redesign) in
`write-tests`. Nine real defects in Search all passed a green gate, and this is
the habit that decided whether each new test was worth anything.

**Not a naive `PostToolUse` hook.** Stashing and restoring a live working tree on
every edit risks the user's uncommitted work, fires far too often, and cannot
tell a regression test from a test for brand-new code.

Likelier shapes:
- a script the workflow calls deliberately at commit time, or
- a `Stop` hook that *checks whether the verification was done* when a diff adds
  tests and changes production code — reporting, not performing.

**When it lands, the Tier 1 rule stays.** Reword it from "do this" to "the hook
checks this, here is what it checks and why". Deleting the rule would leave a
script nobody can explain.

A longer-term version of the same idea is real mutation testing (PIT). The
manual targeted mutation documented in `write-tests` is its hand-run form.

## 2. Run the affected test suite after each agent edit
A Hook covering the Regression pillar that `write-tests` explicitly does not:
that Skill covers Requirements only — "new code does what the Skill or Spec
asked" — and says nothing about whether the change broke something else.

Constraint to respect once screen tests exist: the fast JVM suite and the
Robolectric suite are separate Gradle tasks precisely so the quick loop stays
instant. A hook that runs everything on every edit throws that away.

## 3. Automated drift checker
A Hook or validator subagent implementing `DRIFT-CHECKLIST.md`'s items,
replacing the manual pass. The checklist is already written as violation
detectors ("yes" means drift found), which is the shape an automated checker
needs.

Ranked last because the manual pass currently works and the checklist is short.
Its value grows with the number of documents, not with the amount of code.
