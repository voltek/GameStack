# Tooling backlog

Repository tooling that is planned but not built: automating what is currently a
manual review pass. Kept here, and not in CLAUDE.md, because these are notes
about work not yet done rather than rules constraining work being done now — and
not in the Spec, which describes the product, while this describes the harness
around it.

## What belongs here, and what happens to it
**In:** repository tooling that is planned but not built — hooks, CI, scripts,
automation of a pass currently done by hand. Each entry states what it would do,
which manual step it replaces, where it ranks and why, and any shape already
ruled out with the reason. That last part is the point of the file: it saves the
next attempt from rediscovering a dead end.

**Out:** product features (Spec → Explicitly Deferred), rules that constrain code
on any turn (CLAUDE.md), procedures for a named task (a Skill), and anything
belonging to the human's separate study programme, which is kept outside this
repository entirely.

**When an item ships, it leaves this file.** Its rule, if it has one, goes to
CLAUDE.md; its procedure to a Skill; the incident and the decision to
DRIFT-CHECKLIST's Resolution log. This file only ever holds unbuilt things —
otherwise it quietly becomes a second changelog, and the real one is `git log`.

This is read on demand, not loaded every turn. Consult it when picking up
harness work; there is no need to check it while writing product code.

Four deliverables, in priority order.

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

## 2. CI on GitHub Actions
Run `build`, `test` and `lint` on every push and pull request, so the gate stops
depending on whoever remembered to run it. Nothing exists yet: `main` is
protected but *require status checks* is deliberately switched off, because there
are no checks to require.

Two things to settle when it lands: whether the Robolectric screen-test task runs
on every push or only on PRs (it is slower), and Gradle caching, without which
every run pays a cold build. Turn on *require status checks* in the branch
protection once it is green and stable — that is the whole point of building it.

## 3. Run the affected test suite after each agent edit
A Hook covering the Regression pillar that `write-tests` explicitly does not:
that Skill covers Requirements only — "new code does what the Skill or Spec
asked" — and says nothing about whether the change broke something else.

Constraint to respect once screen tests exist: the fast JVM suite and the
Robolectric suite are separate Gradle tasks precisely so the quick loop stays
instant. A hook that runs everything on every edit throws that away.

## 4. Automated drift checker
A Hook or validator subagent implementing `DRIFT-CHECKLIST.md`'s items,
replacing the manual pass. The checklist is already written as violation
detectors ("yes" means drift found), which is the shape an automated checker
needs.

Ranked last because the manual pass currently works and the checklist is short.
Its value grows with the number of documents, not with the amount of code.
