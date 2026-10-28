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
tooling *not yet built* is TOOLING-BACKLOG; how a drift was resolved is a
`Drift:` trailer on its commit, and a decision in force is an ADR under
`decisions/`. This file does not restate them, and it is not a changelog —
`git log` is the changelog. It
answers the one question none of those do: **what is the harness made of,
today.**

Read on demand — when picking up harness work, or when rebuilding this setup
somewhere else. It is not loaded every turn.

---

## 1. The gate, on CI
**What.** A GitHub Actions workflow named *Gate* whose single job, `build / test /
lint`, runs the three gate commands with `--continue --stacktrace` on every pull
request against `main` and every push to `main`. Reports and test results upload
as an artifact on failure only.

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
**What.** `main` requires the `build / test / lint` and `messages / title / branch`
checks to pass and every review conversation to be resolved. Force-pushes and deletions are blocked; zero
approving reviews are required, since the project has one author. Administrators
are bound too (`enforce_admins`), because the agent pushes with the owner's
administrator credentials (ADR 0028).

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
- *Leaving administrators out to keep an emergency path open.* With zero
  required approvals nothing can lock the owner out, and the exit is one call:
  `gh api -X DELETE repos/:owner/:repo/branches/main/protection/enforce_admins`,
  then `-X POST` to the same path to turn it back on. The agent's settings deny
  that call, so only the owner can use the exit.

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
`DRIFT-LOG.md` is a frozen archive of the drifts resolved before 2026-10-26;
and `decisions/` holds one ADR per decision in force, each naming what enforces
it. Different jobs and growth laws (the log no longer grows) keep them apart.

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
- *A drift log that keeps growing.* Each row was a second copy of the resolving
  commit's body, and nothing read the log to spot a pattern (ADR 0033).

## 8. Claude workflows on GitHub
**What.** Two workflows, and their states differ on purpose. *Claude Code*
(`claude.yml`) is **active**: an on-demand assistant that reacts to a comment
mentioning it. *Claude Code Review* (`claude-code-review.yml`) is **disabled** —
via `gh workflow disable`, reversible with `gh workflow enable`, and free while
off, since Actions bills executed minutes and not files present.

**Why the file is still there.** Code review on this project is internal:
`/code-review`, run by the human, before the PR exists. Its trigger fired on PR
open, while local rounds could still be landing; rather than retrigger it, the
project decided it wants no automated Claude reviewer on GitHub at all
(`DRIFT-LOG.md`, row 2026-08-24, "Installing the Claude GitHub App"). Deleting
the file would lose the configuration; disabling it keeps the
decision reversible. **This entry exists because a present-but-disabled workflow
does not explain itself** — without it, the next reader cannot tell a deliberate
choice from a bug.

**Ruled out.** *Re-triggering it on a mention.* `claude.yml` already fires on any
comment containing that substring, so both workflows would run on the same
comment. A trigger phrase that is not a substring of it avoids the collision.

## 9. Codex review on GitHub
**What.** The ChatGPT Codex connector (`chatgpt-codex-connector[bot]`) reviews
pull requests. A round starts when a PR is opened for review, when a draft is
marked ready, or on a comment `@codex review` or `@codex security review`. While
a round runs it reacts 👀; it then posts findings, or reacts 👍 when it has none.
Its summary comment on each PR names the trigger, the commit and the status.

**Replaces.** Requesting the bot by hand when a PR opened, which `ship-a-branch`
used to call for and which bought a second round on the same commit.

**Where.** Outside the repository, at chatgpt.com/settings/code-review (Review
code → Automatic review). No file here configures it, nothing here turns it on
or off, and no detector checks it.

**Why it stays on while §8's review is off.** §8's decision was about a Claude
reviewer; Codex was a separate bot and was left as it was. ADR 0015 keeps it as
the one automated advisor round per PR, and `ship-a-branch` step 3 says how that
round is used and when to stop waiting for it.

**Ruled out.** *Restating these triggers and signals in the skill.* A copy there
came out incomplete twice; the skill keeps the rule and points here.

## 10. Agent permissions
**What.** `deny` and `ask` rules that stop the agent before a command runs. The
`deny` list covers a push naming `main`, a bare force-push, `--no-verify`, any
change to branch protection, `gh repo edit`/`rename`/`archive`/`delete`,
`gh workflow disable`, `gh secret`, and printing the GitHub token. The `ask`
list covers the common PR actions, deleting a remote branch, `git -C` with
`push` or `commit`, and mutating `gh api` calls, which include turning
auto-merge on. Each rule catches the common spellings of a mistake, never all
of them. TOOLING-BACKLOG item 9 lists the spellings known to get through; where
`main` is concerned, only §2 stops them. A rule also stays narrow enough not to
catch reads: a prompt on every `git -C … log` was dropped for that reason.

**Replaces.** A prompt before every `git push`, and the same rules kept only in a
local file that one machine's global ignore kept out of git.

**Where.** `.claude/settings.json`. Personal `allow` rules stay in
`.claude/settings.local.json`, which `.gitignore` excludes; `deny` and `ask` win
over them. PowerShell denies every `gh api` call, so read GitHub from Bash.

**Ruled out.**
- *Rules for Bash only.* The PowerShell tool runs the same commands, so every
  rule that matters has a `PowerShell(...)` copy. For `gh api`, PowerShell is
  denied outright.
- *A rule ending in `:*` after another wildcard.* Claude Code reads that `*`
  literally, and the rule never fires. A startup warning reports it.
- *Trusting the patterns alone.* A pattern matches one spelling of a command.
  The guard that holds for any spelling is §2's `enforce_admins`.
- *Chasing every spelling.* Each review round widened the patterns and found
  the next gap, and a rule matched anywhere in the command
  (`core.hooksPath`, `-n`) denied reads and commit messages. The rule of three
  stopped it. A hook that parses the command is TOOLING-BACKLOG item 9.

## 11. Commit message and branch name checks
**What.** The mechanical half of ADR 0034 and the trailer forms of ADR 0033:
the subject form, the type and scope against closed lists, a capital first
letter, no trailing period, 72 code points, a blank second line, a body of 12
non-blank lines with known trailers excluded, `Drift:` and `Bypassed:`
values, the branch name's shape, and, on a PR, the title's form, an
"and" in it, and its type against the branch prefix. Every failure names the
rule, the value seen and the fix, and prints the scope list when the scope is
at fault, so the error is the template.

**Replaces.** Reading each subject against `ship-a-branch` § 2 by hand.

**Where.** `.githooks/rules.sh` holds every check and both lists, and
`.githooks/test-rules.sh` its cases. `.githooks/commit-msg` and
`.githooks/pre-push` call it locally; `.github/workflows/commits.yml` runs the
cases, then the checks, when a pull request is opened, reopened, pushed to or
edited.
`installGitHooks` in `app/build.gradle.kts`, run by `preBuild`, sets
`core.hooksPath=.githooks`, warning when that stops other hooks running (another
value, or a hook in `.git/hooks`) or when `git` cannot run; it never fails the
build.

**Ruled out.**
- *A Skill.* Committing is decided mid-work, not requested, so an agent
  cutting corners is the one that would not invoke it.
- *A commit template.* The agent commits with `-F`, which never opens an
  editor, and a pre-filled `Drift:` line is a line that gets left in.
- *A README setup step for the hooks.* A step per clone is a suggestion.
- *The checks inside `gate.yml`.* A title edit would rerun Gradle.
- *Counting the subject in bytes.* An em dash is three bytes.
- *Stripping `#` lines in CI.* Under `-F` git stores them, so CI reads the
  stored message as it is; only the local hook strips the editor's comments.
- *Guessing from the message's shape which lines git will strip.* Three review
  rounds each found a shape it got wrong. git runs commit hooks with
  `GIT_EDITOR=:` exactly when it opens no editor and keeps every comment line,
  so the hook strips them only when that is not set.
- *An opt-out from `installGitHooks`, or installing only when unset.* Either is
  a setup step again, which installing on every build exists to remove; the
  task warns about what it replaces instead.
- *Git's own trailer detection.* `git interpret-trailers` accepts any
  `Key: value` paragraph, so prose such as `Note: …` would leave the count.
- *Checking a PR against `main`'s `rules.sh`.* A new scope joins the list in
  the commit that first uses it (ADR 0034), which `main`'s list would fail.
  An edit to the rules is a reviewed diff, as an edit to `gate.yml` is.

---

## Rebuilding this elsewhere
Order matters only where one piece depends on another:

1. **Version catalog and the Gradle pins** first — everything else assumes a
   build that behaves the same on two machines.
2. **The governing documents** next, even if thin. Rules written after the code
   are rules argued against the code.
3. **The gate workflow** and **`commits.yml`** (entry 11), and let each pass once.
4. **Branch protection** only then, requiring both checks, for the reason in
   entry 2.
5. **The PR template**, and **Skills** as procedures prove themselves repeatable —
   a Skill written before the second run of a task is a guess.

If this section ever grows past ordering into actual steps, it has become a
procedure and belongs in a Skill, not here.
