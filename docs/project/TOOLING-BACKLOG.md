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
CLAUDE.md; its procedure to a Skill; the incident to DRIFT-LOG; the decision to
an ADR under `docs/project/decisions/`; the clauses it now polices to the
`Enforced by:` line of every ADR that states them; and what it now *is* — its
purpose, where it lives, and the shapes ruled out — to TOOLING.md. This file
only ever holds unbuilt things — otherwise it quietly becomes a second
changelog, and the real one is `git log`.

This is read on demand, not loaded every turn. Consult it when picking up
harness work; there is no need to check it while writing product code.

Eight deliverables, in priority order — except items 7 and 8, which are gated on a
trigger rather than ranked.

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

Constraint to respect: Robolectric costs roughly a second **per test**, scaling
rather than amortising, so "run the affected class" is not the cheap option it
sounds like when that class is a screen test. Measure before designing around
it — see CLAUDE.md → Testing Stack for why no figure is written down.

## 3. Automated drift checker
A Hook or validator subagent implementing `DRIFT-CHECKLIST.md`'s items,
replacing the manual pass. The checklist is already written as violation
detectors ("yes" means drift found), which is the shape an automated checker
needs.

Ranked below the two above because the manual pass currently works and the
checklist is short. Its value grows with the number of documents, not with the
amount of code.

**Its input data is the ADR set plus the classified checklist.** The ADRs under
`docs/project/decisions/` already say, per decision, what enforces it, so the
set names which rules are prose only and which have a fitness function. The
per-item classification of this checklist — one `Verified by:` line per item,
from a closed vocabulary — is the other half, and it sorts the items three
ways: `human` is what this checker takes over, `subagent (interpretive)` is
what it never can, and a value naming a tool is covered elsewhere already and
out of its scope. Under *Documentation integrity*, a `Runs when:` line decides
whether an item runs on a given diff at all (ADR 0026), so the checker scopes
its run the same way. The full sweep of CLAUDE.md for enforcement-free rules is
collected when this item is picked up, not before: done earlier it produces a
list nobody is ready to act on.

## 4. Split the Robolectric screen tests into their own Gradle task
Deliberately *not* done when screen tests landed, on a measurement that turned
out to be wrong: the cost scales at roughly a second per screen test rather than
amortising as a per-class startup, and Robolectric is already most of
`testDebugUnitTest`. The trigger is the second screen-test class, which roughly
doubles it.

Shape: a `screenTest` task filtering `*ScreenTest`, with `test` excluding them and
`check` running both. Register it copying `testDebugUnitTest`'s classpath and
`testClassesDirs`; the risk to watch is Gradle's configuration cache, which this
project uses and which does not tolerate reading another task's state at
configuration time.

Do this before, not after, the post-edit hook in item 2 — that hook's whole
premise is a quick loop, and without the split there is no quick loop to protect.

## 5. A `commit-msg` hook for the mechanical half of the message rules
Check `type(scope): imperative summary` against the allowed types, and a body of
12 lines or fewer. Both are regex-checkable, and a git hook runs whether or not
anyone remembers it — CLAUDE.md already forbids `--no-verify`, so it cannot be
skipped either.

**A Skill was considered and rejected for this.** Skills load when a named task
begins, and committing is decided by the agent mid-work rather than requested;
an agent cutting corners is precisely the one that would not invoke it. That
leaves a Skill on the same rung of the ladder as the prose, plus indirection. A
hook is a rung up.

Scope limit worth stating before anyone tries: the hook can only see **form**.
"Does this commit exist because of this branch's unit of work" and "does the body
carry the diagnosis" are judgement, and the rules covering them stay in CLAUDE.md
regardless.

## 6. Make the project-coupled Skills exportable — lowest priority
**No benefit to this project.** Purely to lift Skills into a separate reusable
collection, so it happens only when nothing else is queued.

An audit on 2026-08-23 classified all fourteen:

- **Portable as-is:** `ship-a-branch`, `discovery-feature` — no project or
  platform coupling at all.
- **Android-generic:** `verify-on-device`, `new-usecase`,
  `new-repository-interface`, `new-viewmodel`, `new-screen`,
  `new-repository-impl`, `new-hilt-module` — coupled to the platform, not to
  GameStack. Already lifted.
- **Mixed — the work is here:** `write-tests` (the regression-verification and
  mutation recipes are language-agnostic; `GameRatingConverters`,
  `AuthInterceptor` and `com.gamestack` paths are not), `new-mapper`,
  `new-room-dao`, and `new-feature`, which is coupled by design as the
  orchestrator.
- **Not portable:** `new-api-service` — Apicalypse is IGDB's own query language.

Do not "generalise" these in place. A Skill that has stopped naming this
project's real types is worse *here*, and the examples are what make them usable.
Extract a copy instead.

## 7. Check the ADR index against `decisions/` — once an index exists
**Not gated behind items 1 and 2.** Comparing two lists of strings interprets
nothing, so this is not the drift-auditor of item 3 wearing another hat and does
not wait behind it.

**What it does.** Reads the ADR files in `docs/project/decisions/` and asserts
that whatever index names them agrees: same numbers, same titles, nothing listed
that no longer exists, and nothing on disk missing from the list.

**Replaces.** Re-reading both by eye — the step that reliably goes unrun.

**The trigger is concrete**, and it has not fired: there is deliberately no index
today, because `grep -H "^Enforced by:" docs/project/decisions/[0-9]*.md`
answers the question from a terminal. It fires at roughly 30 ADRs, or the first
time the map is wanted *inside a document* rather than in a terminal, whichever
comes first. That is when an index earns its keep — and this check lands with it, in
the same change, not afterwards.

**Ruled out.** *Keeping the index by hand.* CLAUDE.md's Skills list drifted out
of sync with `.claude/skills/` in exactly that way and had to be given its own
drift detector. An index nobody verifies is just a second source of truth.

## 8. Fix the gate's Gradle-cache packaging failure — once it recurs
**What it does.** Stops the gate going red in `:app:packageDebug` or
`:app:packageRelease` with `PackageAndroidArtifact$IncrementalSplitterRunnable`
while tests and lint pass, on a diff that cannot touch packaging. It hit both
attempts of PR #38's one run, after a restored `gradle-home-v2|…|gate[…]` entry,
and stopped once that entry was deleted. The `transforms` and `dependencies`
entries it restored passed under a fresh `gradle-home` entry, so they are not
sufficient on their own; a combination with a stale one is not ruled out.

**Replaces.** Deleting the entry by hand (`gh cache list`, `gh cache delete <id>`,
with the owner's approval) each time it happens.

**The trigger is concrete**, and it has not fired again: the next occurrence. The
gate runs with `--stacktrace`, so that run carries the evidence the first one
never printed. Before deleting the entry, read the trace and keep the run id and
the entry's `gh cache list` line (key, size, dates): deletion destroys the entry,
so that is all of it that survives. Then pick the fix.

**Candidate shapes**, each to be chosen by the trace, not before it: excluding
transforms from the cache, or a cache-key bump.

**Ruled out.**
- *Choosing a cache change without a trace.* Each candidate answers a cause
  nobody established, and the deleted entry can no longer be inspected.
- *Making `main` cache-read-only.* PR jobs already are, so no job would save:
  existing entries keep being restored and never refreshed, a bad one included.

## 9. A `PreToolUse` hook that judges git and gh commands by parsing them
**What it does.** Reads each Bash and PowerShell command before it runs, strips
git's global options (`-C`, `-c`, `--no-pager`, `--git-dir`, `--work-tree`),
splits combined short flags, and judges the subcommand: deny a push that reaches
`main` (including a bare `git push` from `main`), a force-push without a lease,
and skipping hooks; ask for PR actions and remote-branch deletion.

**Replaces.** The `deny` and `ask` patterns in `.claude/settings.json`, which
match one spelling each (TOOLING §10). Those stay as the fallback.

**Ranking.** Belongs with the hooks work (item 5 and items 1-2). It matters most
for feature branches, where branch protection does not reach.

**Before it ships:** test it against one planted command per spelling below.
Every one of them got past the patterns.
- Global options before the subcommand: `git --no-pager push -f origin x`,
  `git -c k=v push --force origin x`, `git -c core.hooksPath=/dev/null commit`,
  `--git-dir`, `--work-tree`.
- Combined or short flags: `-vf`, `-qf`, a trailing `-fu`/`-uf`, `git commit -n`,
  `-an`, `-anm`.
- Reaching `main` without naming it: a bare `git push` or `git push origin HEAD`
  from `main`, or a quoted `'main'` or `"HEAD:main"`.
- Deleting a branch outside the four prefixes: `git push origin :hotfix`,
  `:refs/heads/fix/x`.
- `gh` commands outside the lists: `gh pr revert`, `lock` and `unlock`;
  `gh variable`, `gh repo deploy-key`, `gh label`, `gh workflow enable` and
  `gh cache delete`; and a `gh api` whose field comes before the path.

It must also let these reads through, since each was wrongly caught on the way:
`git config --get core.hooksPath`, a commit message that mentions `--no-verify`,
`git -C <path> log`, and a GraphQL query sent with `-f query=`.

**Ruled out.**
- *More patterns.* Each of three review rounds on `fix/agent-permissions` found
  either the next gap or a rule wide enough to catch reads; the rule of three
  closed that route.

## 10. Check CLAUDE.md's size bound in the build — once it meets it
A Gradle task that `check` depends on, counting as
[ADR 0031](decisions/0031-claude-md-is-bounded.md) § Decision defines, so the
gate stays three commands. **Gated on a trigger:** the first commit in which
CLAUDE.md meets the bound, since a check added earlier would only fail. A
ratchet for the interim rule was declined (ADR 0031 § Consequences).
