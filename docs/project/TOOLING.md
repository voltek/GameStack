# Tooling — what this repository is made of

The harness around the product: what enforces the rules, what automates a pass
that used to be manual, and what was deliberately ruled out along the way. One
entry per piece, each naming the file that actually configures it.

## What this file is, and what it is not
**In:** repository tooling that exists *right now* — its purpose, the manual step
it replaced, where it is configured, and the shapes rejected on the way to it.
That last field is the one worth writing: it is what stops the next project from
walking into the same dead end.

**Out:** anything that already has a home. Rules constraining code on any turn are
in CLAUDE.md; procedures for a named task are Skills; the product is the Spec;
tooling *not yet built* is TOOLING-BACKLOG; how a drift was found and resolved is
DRIFT-LOG, and a decision in force is an ADR under `decisions/`. This file does
not restate them, and it is not a changelog — `git log` is the changelog. It
answers the one question none of those do: **what is the harness made of,
today.**

Read on demand — when picking up harness work, or when rebuilding this setup
somewhere else. It is not loaded every turn.

---

## 1. The gate, on CI
**What.** A GitHub Actions workflow named *Gate* whose single job, `build / test /
lint`, runs the three gate commands with `--continue` on every pull request
against `main` and every push to `main`. Reports and test results upload as an
artifact on failure only.

**Replaces.** Remembering to run the three commands by hand, and trusting that
everyone else did.

**Where.** `.github/workflows/gate.yml`.

**Ruled out.**
- *Caching only `~/.gradle`.* Robolectric fetches its ~204MB SDK jar at **test**
  runtime through its own Maven client, into `~/.m2`, never through Gradle's
  classpath. `~/.m2/repository/org/robolectric` is cached separately.
- *Declaring that jar as a `testImplementation` dependency.* It downloads a
  second copy nothing reads. The supported route, if caching ever stops being
  enough, is `robolectric.offline=true` plus `robolectric.dependency.dir`.
- *The combined `actions/cache`.* It saves in a post step that is skipped when
  the job fails, which is exactly when the next run comes soonest. Split into
  `cache/restore` and `cache/save`.
- *Guarding that save with `always()`.* A run cancelled by the concurrency group
  can persist a half-written jar under the primary key, where it reads as an
  exact hit and is never overwritten. Uses `!cancelled()`.
- *Wrapping the cache key in a folded scalar.* YAML keeps literal newlines inside
  the expression unless every continuation line sits at the same indent. The key
  stays on one line.

## 2. Branch protection on `main`
**What.** `main` requires the `build / test / lint` check to pass and every review
conversation to be resolved. Force-pushes and deletions are blocked; zero
approving reviews are required, since the project has one author.

**Replaces.** The honour system — a red branch was previously merge-able.

**Where.** GitHub, Settings, Branches. Readable as JSON at
`gh api repos/:owner/:repo/branches/main/protection`. Creating the status-check
requirement needs a `PUT` of the **whole** protection object; the dedicated
`PATCH` sub-endpoint only works once it already exists, so a careless call
silently drops conversation resolution.

**Ruled out.**
- *Switching required checks on before the first green run.* A required check
  that has never reported blocks every merge, including the one that would
  introduce it. Turn it on after the workflow has passed once.
- *`strict: true`*, requiring the branch to be up to date before merging. For
  `pull_request` events GitHub already runs the workflow against the merge
  commit, so the check reflects `main` as of when it ran. `strict` only closes
  the gap between that moment and the merge, at the cost of re-updating every
  open PR whenever `main` moves.

## 3. Pull request template
**What.** Three sections — what changed, why, how it was verified — with the gate
and `/code-review` as explicit checkboxes, and an inline note on where to paste
screenshots so they are never committed to the repo.

**Replaces.** Deciding what a PR body should contain, once per PR.

**Where.** `.github/pull_request_template.md`.

## 4. Gradle version catalog
**What.** Every dependency and plugin version declared once, in TOML, referenced
by alias from the build scripts.

**Replaces.** Version strings scattered across build files, drifting apart.

**Where.** `gradle/libs.versions.toml`.

**Ruled out.** *Trusting a remembered version when adding a library.* Maven
Central's search endpoint does not sort by version, so its first page can look
current and not be. Read `maven-metadata.xml` and sort.

## 5. Reproducible Gradle builds
**What.** Three separate pins, each closing a different hole: the wrapper
distribution is checksum-verified via `distributionSha256Sum`, the daemon JVM is
pinned to Java 21 by criteria rather than by whatever is on `PATH`, and the
configuration cache is on.

**Replaces.** "Works on my machine" — and, on CI, a toolchain download per job.

**Where.** `gradle/wrapper/gradle-wrapper.properties`,
`gradle/gradle-daemon-jvm.properties`, `gradle.properties`.

**Ruled out.** *Letting CI pick its own JDK.* The daemon criteria file is what
makes `actions/setup-java` with `java-version: 21` the right answer rather than a
guess; a mismatch means Gradle downloads a toolchain on every job.

## 6. Skills
**What.** Fourteen procedures in `.claude/skills/`, one per repeatable task —
each layer's scaffolding, tests, device verification, and shipping a branch. A
Skill loads only when invoked, which is why procedures live there instead of in
CLAUDE.md, which is loaded on every turn.

**Replaces.** Improvising the same task a slightly different way each time.

**Where.** `.claude/skills/`, and `docs/project/retired-skills/` for those taken
out of the listing.

**Ruled out.** *Keeping a once-per-project skill invocable.* `project-scaffold`
bootstrapped this repo and was retired afterwards — it offered an agent something
it should never pick. It is archived rather than deleted, because how the project
was bootstrapped is worth keeping.

## 7. The governing documents
**What.** Five files and a folder, with disjoint jobs: `CLAUDE.md` holds rules
that constrain code on any turn, under a three-tier system saying what may be
changed and who must agree; the Spec holds the product; `DESIGN.md` holds the visual system;
`DRIFT-CHECKLIST.md` holds the violation detectors you run to *find* drift;
`DRIFT-LOG.md` holds the incidents — every time code and documentation
disagreed, and which of the two was changed; and `decisions/` holds one ADR per
decision in force, each naming what enforces it. They grow at different rates,
which is why they are separate files.

**Replaces.** Rediscovering, per session, why the code looks the way it does.

**Where.** `CLAUDE.md`, `docs/project/GameStack-Spec-v1.md`,
`docs/project/DESIGN.md`, `docs/project/DRIFT-CHECKLIST.md`,
`docs/project/DRIFT-LOG.md`, `docs/project/decisions/`.

**Ruled out.**
- *Putting everything in CLAUDE.md.* Every line there is paid for on every
  request, whether or not the task touches it. The test for admission is not
  "is this valuable" but "is this needed on every turn".
- *A hand-written index of the ADRs.* A second place to update per ADR, and the
  Skills list in CLAUDE.md already drifted out of sync with `.claude/skills/`
  exactly that way. `grep -H "^Enforced by:"` reads the set instead.

## 8. Claude workflows on GitHub
**What.** Two workflows, and their states differ on purpose. *Claude Code*
(`claude.yml`) is **active**: an on-demand assistant that reacts to a comment
mentioning it. *Claude Code Review* (`claude-code-review.yml`) is **disabled** —
via `gh workflow disable`, reversible with `gh workflow enable`, and free while
off, since Actions bills executed minutes and not files present.

**Why the file is still there.** Code review on this project is internal:
`/code-review`, run by the human, before the PR exists. The bot triggered on PR
open, which is *after* those rounds finish, so it commented on findings already
fixed. Deleting the file would lose the configuration; disabling it keeps the
decision reversible. **This entry exists because a present-but-disabled workflow
does not explain itself** — without it, the next reader cannot tell a deliberate
choice from a bug.

**Ruled out.** *Re-triggering it on a mention.* `claude.yml` already fires on any
comment containing that substring, so both workflows would run on the same
comment. A trigger phrase that is not a substring of it avoids the collision.

---

## Rebuilding this elsewhere
Order matters only where one piece depends on another:

1. **Version catalog and the Gradle pins** first — everything else assumes a
   build that behaves the same on two machines.
2. **The governing documents** next, even if thin. Rules written after the code
   are rules argued against the code.
3. **The gate workflow**, and let it pass once.
4. **Branch protection** only then, for the reason in entry 2.
5. **The PR template**, and **Skills** as procedures prove themselves repeatable —
   a Skill written before the second run of a task is a guess.

If this section ever grows past ordering into actual steps, it has become a
procedure and belongs in a Skill, not here.
