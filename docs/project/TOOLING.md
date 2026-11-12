# Tooling — what this repository is made of

The harness around the product: what enforces the rules, what automates a pass
that used to be manual, and what was deliberately ruled out along the way. One
entry per piece, each naming the file or setting that actually configures it.

## What this file is, and what it is not
**In:** repository tooling that exists *right now* — its purpose, the manual step
it replaced, where it is configured, and the shapes rejected on the way to it.
That last field is the one worth writing: it is what stops the next project from
walking into the same dead end.

**Out:** anything that already has a home. Rules constraining code on any turn are
in CLAUDE.md; procedures for a named task are Skills; the product is PRODUCT.md;
tooling *not yet built* is a GitHub issue (ADR 0038); how a drift was resolved
is a `Drift:` trailer on its commit, and a decision in force is an ADR under
`decisions/`. This file does not restate them, and it is not a changelog —
`git log` is the changelog. It
answers the one question none of those do: **what is the harness made of,
today.**

**Admission:** an entry exists only for a piece configured by a file or setting
it names under **Where.**, or, for a service configured outside the repository,
the place it is set (entry 9), and it states the step it replaced, a shape it
ruled out, or both. A state of the repository with no lesson is not an entry
([ADR 0040](decisions/0040-the-harness-has-a-base-and-a-tooling-entry-a-lesson.md)).

**Base:** the harness is built for a Gradle project for Android or Kotlin
Multiplatform, with git and a shell; nothing is generalised past it. The forge,
the agent CLI and the model may change, so a piece takes the mechanism that
needs none of them when it costs about the same; otherwise it is built on the
tool, and an adopter translates it. No existing piece is rebuilt only to drop a
tool.

Read on demand — when picking up harness work, or when rebuilding this setup
somewhere else. It is not loaded every turn.

---

## 1. The gate, on CI
**What.** A GitHub Actions workflow named *Gate* whose single job, `build / test /
lint`, runs the three gate commands with `--continue --stacktrace` on every pull
request against `main` and every push to `main`. Reports and test results upload
as an artifact whenever the gate step does not succeed; a GC log per JVM, its
heap ceiling included (`gc+init`), uploads on every run. The daemon heap is `-Xmx4g` (`gradle.properties`), a mitigation
rather than a measured size: release packaging deflates a 32 MB `classes.dex` in
one buffer while lint runs in the same daemon, and at 2048m that failed with
`OutOfMemoryError` or hung on CI. The Kotlin daemon inherits the same `-Xmx`.

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
- *Keying on only one of the two files.* `robolectric.properties` picks which
  SDK jar is fetched, `libs.versions.toml` the Robolectric version that fetches
  it. Miss either and a bump keeps the old key, which `actions/cache` reports as
  an exact hit, so it skips the save and re-downloads ~204MB on every job.
- *Wrapping the cache key in a folded scalar.* YAML keeps literal newlines inside
  the expression unless every continuation line sits at the same indent. The key
  stays on one line.
- *Treating that packaging failure as a stale Gradle cache.* The same restored
  entries hung one attempt and passed the next on the same commit; the
  `--stacktrace` named the daemon's heap. Deleting cache entries fixes nothing.
- *Sizing CI's heap from a local run.* Locally the gate passed at 768m, and at
  1280m with no cached configuration, as on CI; CI still ran out at 2048m. Only
  CI's own GC logs size it, a passing run's included.
- *Uploading on `failure()` and relying on the job timeout.* A job timeout
  cancels the job, and the docs say nothing about which status function a
  timeout satisfies. The gate step gets a 20-minute timeout of its own, about
  twice the slowest healthy run, so the job outlives a hang and the reports
  upload by checking that step's `outcome`.
- *A CI-only heap override.* `org.gradle.jvmargs` overrides as a whole value, so
  a copy on the CI command line drifts from `gradle.properties`, and the local
  gate stops running the daemon CI runs.

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
**What.** One shape for a PR body: what changed, why, the issues it closes, and
how it was verified, then the review: the rounds run and each fix made after the
last round, so the first screen holds the claim and the owner sees what to read
at the merge. `commits.yml` checks it (entry 11).

**Replaces.** Deciding what a PR body should contain, once per PR, and reading
each one for an unwritten review.

**Where.** `.github/pull_request_template.md`.

**Ruled out.**
- *Boxes for the gate and `/code-review`.* The gate's was ticked on 55 of 55 PRs:
  `gate.yml` is a required check, and the written review replaces the other.
- *Listing which detectors ran.* The diff and each item's `Runs when:` say so;
  the line says what they found.
- *Issues inside `<details>` or a comment.* GitHub does not say whether a closing
  keyword counts there.
- *Sample values in the review or in Issues.* A sample left in reads as real, and
  a real number would close; the template's `TODO` or `#n` left in keeps the
  check red.
- *A line per accepted and dismissed finding, cited by its triage id.* The id
  resolved to nothing once the tables were deleted, the reviewer never read the
  lines, and the owner had confirmed each finding already (ADR 0063).

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
**What.** Procedures in `.claude/skills/`, one per repeatable task —
each layer's scaffolding, tests, device verification, and shipping a branch in
four moments (`clean-branch`, `review-round`, `open-pr`, `merge-pr`) —
plus the `openspec-*` Skills OpenSpec generates (entry 13). A
Skill loads only when invoked, which is why procedures live there instead of in
CLAUDE.md, which is loaded on every turn. A rare case goes to a file under the
Skill's `references/`, which its `SKILL.md` says when to read.

**The shipping Skills' bound.** `ShippingSkillBoundTest` fails the gate when one
of the four `SKILL.md` files passes 15,000 bytes (ADR 0060): after a compaction
Claude Code re-attaches only the first 5,000 tokens of an invoked Skill, so a
longer one loses its last steps mid-task.

**A Skill's scripts.** A step that must come out the same every time is a script
under the Skill's `scripts/`, with its test beside it, run in `commits.yml`.
`review-round/scripts/save-review.mjs` saves a `/code-review` round's raw text
from the subagent's transcript, the only literal copy: the subagent's output file
is empty, and its notification escapes `<` and `>`. It saves into the checked-out
branch's `reviews/<branch>/`, so the folder cannot be misnamed into one the
SessionStart hook deletes (ADR 0067). The transcript format is not
documented, so every unexpected shape exits non-zero with a repair notice, and the
Skill falls back to the notification, saying so on the record's first line.
`clean-branch/scripts/reword.sh` makes the `amend!` commit that rewords an earlier
one. git refuses `-F` and `-m` with `--fixup=reword`, so the message has to pass
through an editor, and an editor that overwrites the file drops git's marker line:
the result is an ordinary commit that `--autosquash` leaves alone, with no error.
The script keeps the marker and fails when the commit it made is not an `amend!`.

**Replaces.** Improvising the same task a slightly different way each time.

**Where.** `.claude/skills/`, and `docs/project/retired-skills/` for those taken
out of the listing.

**Ruled out.**
- *Keeping a once-per-project skill invocable.* `project-scaffold`
  bootstrapped this repo and was retired afterwards — it offered an agent something
  it should never pick. It is archived rather than deleted, because how the project
  was bootstrapped is worth keeping.
- *One Skill for shipping a branch.* Its description promised one task while it
  ran four, it grew past 500 lines, and after a compaction only its opening
  steps came back, so a review round was skipped twice. Each moment now has a
  Skill whose description names when it applies.
- *Supporting files alone, under one `SKILL.md`.* A file read once does not come
  back after a compaction either, and the description still named one task.
- *The extractor in shell.* Git Bash has no `jq`, and `sed` cannot decode a JSON
  string's escapes, which a review about markup is full of. Node parses JSON and
  ships on the CI runner.
- *Copying the raw text by hand.* It cost about 2,000 output tokens a round and
  saved the escaped form, which on a branch about HTML comments and `<details>`
  no longer matched what the reviewer wrote.

## 7. The governing documents
**What.** Five files and two folders, with disjoint jobs: `CLAUDE.md` holds rules
an agent needs on any turn, and `.claude/rules/` holds those that bind one kind
of file, loaded by path when one is touched; `PRODUCT.md` holds the product; `DESIGN.md` holds the visual system;
`DRIFT-CHECKLIST.md` holds the violation detectors you run to *find* drift;
`DRIFT-LOG.md` is a frozen archive of the drifts resolved before 2026-10-26;
and `decisions/` holds one ADR per decision in force, each naming what enforces
it. Different jobs and growth laws (the log no longer grows) keep them apart.

**CLAUDE.md's bound.** `ClaudeMdBoundTest` fails the gate past ADR 0055's 60 lines and
3,600 characters. CLAUDE.md is declared an input of the test tasks, because a test that
opens a file by path otherwise stays UP-TO-DATE when that file changes.

**Replaces.** Rediscovering, per session, why the code looks the way it does.

**Where.** `CLAUDE.md`, `.claude/rules/`, `docs/project/PRODUCT.md`,
`docs/project/DESIGN.md`, `docs/project/DRIFT-CHECKLIST.md`,
`docs/project/DRIFT-LOG.md`, `docs/project/decisions/`.

**Measuring a line.** `.claude/rules/governance.md` drops a line with no effect, and a
comparison shows that. Give the same prompt through `claude -p`, in a fresh clone per
run with a fake origin, once with the line and once without. Read what the agent did
from `--output-format stream-json`, not what it says. Use the real gate, since agents
notice a stand-in. Pass `--model` and `--effort` explicitly and name them in the record,
because a result holds for those. The task must tempt the agent into the behaviour the
line prevents, or "no effect" means nothing. Two runs per side show only large
differences.

**Ruled out.**
- *Putting everything in CLAUDE.md.* Every line there is paid for on every
  request, whether or not the task touches it. The test for admission is not
  "is this valuable" but "is this needed on every turn".
- *A hand-written index of the ADRs.* A second place to update per ADR, and a
  Skills list CLAUDE.md once held drifted out of sync with `.claude/skills/`
  exactly that way. `grep -H "^Enforced by:"` reads the set instead.
- *A drift log that keeps growing.* Each row was a second copy of the resolving
  commit's body, and nothing read the log to spot a pattern (ADR 0033).

## 8. Claude workflows on GitHub
**What.** Two workflows, and their states differ on purpose. *Claude Code*
(`claude.yml`) is **active**: an on-demand assistant that reacts to a comment
mentioning it. *Claude Code Review* (`claude-code-review.yml`) is **disabled** —
via `gh workflow disable`, reversible with `gh workflow enable`, and free while
off, since Actions bills executed minutes and not files present.

**Where.** `.github/workflows/claude.yml` and
`.github/workflows/claude-code-review.yml`; whether each is enabled is
repository state, read with `gh workflow list --all`.

**Why the file is still there.** Code review on this project is internal:
`/code-review`, launched by the agent on the human's yes (`review-round`), before
the PR exists. Its trigger fired on PR open, while local rounds could still be
landing; rather than retrigger it, the project decided it wants no automated
Claude reviewer on GitHub at all
(`DRIFT-LOG.md`, row 2026-08-24, "Installing the Claude GitHub App"). Deleting
the file would lose the configuration; disabling it keeps the
decision reversible. **This entry exists because a present-but-disabled workflow
does not explain itself** — without it, the next reader cannot tell a deliberate
choice from a bug.

**When a reviewer on the PR fits better.** Local rounds suit one owner with an
agent session: findings reach that session's chat, and the triage happens there.
A reviewer on the PR posts inline comments and a check run that everyone with
access reads, and it runs without anyone's session, so it fits several people
reviewing, or contributors with no agent. Two forms, per Claude Code's Code Review
and GitHub Actions documentation:
- *Managed Code Review*: Team or Enterprise plans, billed to the organization
  as usage credits outside the plan's usage; read-only, and its check never fails,
  so it never blocks a merge.
- *`claude-code-action`*: a workflow of the repository's own, on Actions minutes
  plus API tokens or a subscription's OAuth token; failing the job can gate a merge.

Either runs after the PR exists, so adopting one as the main reviewer means
rewriting ADR 0014's rounds before the PR and `review-round`, not only a trigger.

**Ruled out.** *Re-triggering it on a mention.* `claude.yml` already fires on any
comment containing that substring, so both workflows would run on the same
comment. A trigger phrase that is not a substring of it avoids the collision.

## 9. Codex review on GitHub
**What.** The ChatGPT Codex connector (`chatgpt-codex-connector[bot]`) reviews
pull requests. A round starts when a PR is opened for review, when a draft is
marked ready, or on a comment `@codex review` or `@codex security review`. While
a round runs it reacts 👀; it then posts findings, or reacts 👍 when it has none.
Its summary comment on each PR names the trigger, the commit and the status, and
it is posted when the PR opens, with the status "running": a script that waits
for the round reads `codex-review-state` in that comment, a review or the 👍,
never a count of comments. When its quota is spent it posts a usage-limit
comment instead. Automatic review sits
behind two switches, the repository's and each user's personal preference, so a
round can fail to start for a reason nobody in the repository can see.

**Replaces.** Requesting the bot by hand when a PR opened, which the shipping
Skill used to call for and which bought a second round on the same commit.

**Where.** Outside the repository, at chatgpt.com/settings/code-review (Review
code → Automatic review). No file here configures it, nothing here turns it on
or off, and no detector checks it.

**Why it stays on while §8's review is off.** §8's decision was about a Claude
reviewer; Codex was a separate bot and was left as it was. ADR 0049 keeps it as
the one automated advisor round per PR, and `open-pr` § 5 says how that round is
used and when to stop waiting for it (ADR 0058).

**Ruled out.** *Restating these triggers and signals in a Skill.* A copy there
came out incomplete twice; the Skill keeps the rule and points here.

## 10. Agent permissions
**What.** `deny` and `ask` rules that stop the agent before a command runs. The
`deny` list covers a push naming `main`, a bare force-push, `--no-verify`, any
change to branch protection, `gh repo edit`/`rename`/`archive`/`delete`,
`gh workflow disable`, `gh secret`, and printing the GitHub token. The `ask`
list covers the common PR actions, deleting a remote branch, `git -C` with
`push` or `commit`, and mutating `gh api` calls, which include turning
auto-merge on. An `Edit` rule denies the file tools on the generated
`openspec-*` Skills (entry 13). Each rule catches the common spellings of a mistake, never all
of them. Issue #71 lists the spellings known to get through; where
`main` is concerned, only §2 stops them. A rule also stays narrow enough not to
catch reads: a prompt on every `git -C … log` was dropped for that reason. The
`deny` on a push naming `main` fires in a scratch repository too, so scratch
history is built without that push.

Auto mode's classifier is a separate gate: `permissions.allow` does not stop it,
and its own `autoMode` rules are read only from user or managed settings or
`--settings`, never from a project's files (`claude auto-mode config` shows what
it reads). It judges each call rather than applying a list: it has denied some
`git commit --amend` calls as destructive and passed most, so `clean-branch`
§ Folding does not depend on it and uses marker commits (ADR 0059).

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
- *`autoMode` rules in `.claude/settings.json` or `settings.local.json`.* The
  classifier ignores them. The "Teach auto mode about your environment?" prompt
  writes into user settings, so its lines reach every project: answer it "Don't
  show again".
- *Chasing every spelling.* Each review round widened the patterns and found
  the next gap, and a rule matched anywhere in the command
  (`core.hooksPath`, `-n`) denied reads and commit messages. The rule of three
  stopped it. A hook that parses the command is issue #71.

## 11. Commit message and branch name checks
**What.** The mechanical half of ADR 0034 and the trailer forms of ADR 0033:
the subject form, the type and scope against closed lists, a capital first
letter, no trailing period, 72 code points, a blank second line, a body of 12
non-blank lines with known trailers excluded, `Drift:` and `Bypassed:`
values, the closed list of exempt subjects with the fold markers `fixup!`,
`squash!` and `amend!` accepted by the local hook alone (ADR 0059), the
branch name's shape, a typed subject that two commits on the branch share,
checked at the push and on a PR (ADR 0062), and, on a PR, the title's form, its type against
the branch prefix, and the form of its body with no triage id (ADRs 0057 and
0063). In a message, a title and a body it rejects the references no clone can
open that a pattern recognises: a plan's branch label, a cycle, a `plans/` path,
the handoff's directory, a Windows home written in full, and a triage id
(ADR 0064). Every failure
names the rule, the value seen and the fix, and prints the scope list when the
scope is at fault, so the error is the template.

**Replaces.** Reading each subject against the written form by hand.

**Testing the hooks.** `test-rules.sh` covers `rules.sh`. A commit made to see
the hook pass is a real commit, so make it in a scratch clone, never with
`--no-verify`; in that clone, point `core.hooksPath` at a copy of the changed
hooks outside the work tree, since `git reset --hard` restores the tracked
`rules.sh`. A run of `test-rules.sh` against a planted defect takes about two
minutes: run it in the background, on a copy of `.githooks` and `.github`. A
node script that plants one passes the replacement as a function, since in a
replacement string `$'` inserts the rest of the file.

**Where.** `.githooks/rules.sh` holds every check and both lists, and
`.githooks/test-rules.sh` its cases. `.githooks/commit-msg` and
`.githooks/pre-push` call it locally; `.github/workflows/commits.yml` runs the
cases, then the checks, when a pull request is opened, reopened or pushed to,
and on any edit of its title or body, or of its base onto `main`. The same job
runs the other harness scripts' tests (entries 6 and 14), so a red
`messages / title / branch` names the failing step.
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
- *Skipping the run when only the body changed.* A job skipped by its `if`
  reports Success to a required check, so a body edit after a red run would
  turn it green unchecked. The run is a shell script, not Gradle.
- *Exempting fold markers in CI too.* A marker left unfolded would reach `main`
  as a commit nobody meant to keep. Folding by replaying commits with
  `cherry-pick -n` went with it: it broke on a merge of `main`, which
  `git rebase --autosquash --rebase-merges` rebuilds.
- *Counting the subject in bytes.* An em dash is three bytes.
- *The repeated subject in `commit-msg`.* git gives that hook no sign of an
  `--amend`, not even to `prepare-commit-msg` under `--amend -m`, so an amend
  that keeps its subject would match the commit it replaces. At the push the
  amended commit is already gone.
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
- *Failing a PR title that holds an "and".* A script sees the word, not whether
  one claim or two stands behind it, so a title for one claim that named two
  things failed, and each failure cost a title edit and a rerun.
- *Bounding a PR body's review.* It comes after the claim, and a long list of
  fixes no round read is for the owner to see, not to trim (ADR 0063).
- *Checking each line of a PR body's review.* Markdown writes a list many ways,
  and two review rounds of line patterns rejected valid lists and let prose
  through; the check reads the first line and the template's samples only.
- *Checking the repository's files for references.* The `checkpoint` Skill and
  this file name the handoff's directory on purpose; the CLAUDE.md line covers
  files (ADR 0064).
- *A bare `item N` among the references.* A document's own list ("item 3" of a
  repository file) is a valid reference, so only `cycle N` is matched.

## 12. Issue form
**What.** One form for every pending item (ADR 0038): the title in the commit
form, then source, problem with evidence, screenshots, grounds, suggested fix,
ruled out, trigger, where it lands, bucket, priority and export value. Blank
issues are off, so a person opening an issue on GitHub gets the form too. Its
author sets the bucket, a `p1`–`p3` unless the item is `gated`, `export` and
`gated`; a triage confirms or changes them (ADR 0047). Product work has a
second form (ADR 0043): source, what the user gets, grounds in `PRODUCT.md`,
notes and a proposed horizon, labelled `product`. An app defect has a third
(ADR 0048): source, steps with expected and actual, device and build,
screenshots, grounds and a proposed horizon, labelled `product` and `bug`. A
product issue's horizon is the Horizon field (Now, Next, Later) of the
`GameStack Product` GitHub Project, which holds product issues only and adds
each one by its label; its opener sets the horizon and a triage confirms it.

**Replaces.** An issue body written however its author wrote it that day, and
`TOOLING-BACKLOG.md`, the file that held unbuilt tooling until each of its items
became an issue; for product, the Spec's *Explicitly Deferred* section; for an
app defect, the harness form and its `defect` bucket.

**Where.** `.github/ISSUE_TEMPLATE/backlog.yml`, `product.yml`, `bug.yml` and
`config.yml`. The labels' names and meanings are ADR 0038 § Decision, ADR 0043
and ADR 0048; on GitHub they are repository state that a new repository creates
once (`gh label create`), `bug` included, since a form drops a label that does
not exist; and so are the Project and its field (`gh project
create`, `gh project field-create`, `gh project link`; the token needs the
`project` scope). The Project's auto-add workflow, filter `is:issue
label:product`, is set by hand in its *Workflows* settings: GitHub Free allows
one, and it adds an older issue only once that issue is next updated.

**Ruled out.**
- *A backlog file.* Cheaper for an agent to read, but it grows the way
  TOOLING-BACKLOG did, links to no pull request, and binds nobody opening work
  by hand.
- *The file kept, with one issue per item.* A priority in a label and a
  description in a file is a hand-kept index: two places per item.
- *The bucket as a form field alone.* A dropdown cannot apply a label, and the
  author's bucket is a proposal; the label records the triage's call.
- *Horizon as a label.* Readable by `gh issue list` with no extra scope, but a
  Project's board groups by its own fields, not by labels.
- *Harness issues in the product Project.* They would carry two priorities, a
  `p1`–`p3` label and a Horizon, for one item.
- *A harness Project.* A board or a view groups by a single-select or iteration
  field, never by a label, so it needs fields that mirror the labels (a second
  source of priority) or replace them (`gh issue list` and the hook go blind).
  The triage's grouped read covers it; reopens when more than one person works
  the pool, or a view can group by label.
- *App defects in the harness pool under `defect`*, or in one product form
  widened to cover them: the counts mix, or optional fields fit neither kind.
- *An issue type `bug`*, defined per organization only, or *a form's
  `projects:` key*, which covers that form alone and writes one owner's Project
  number into every clone.

## 13. Spec-driven changes
**What.** OpenSpec (ADR 0041): a spec per capability under `openspec/specs/`,
changed by one change folder per feature branch, verified before the reviews
and archived before the merge. Seven of its generated Skills run the steps; the project schema
adds a verifier to every scenario, assumptions and open questions to every
proposal, the layer Skills to every task list, and makes a design optional.

**Replaces.** A screen's behaviour as bullets in the Spec (now PRODUCT.md),
written once before the build, with the states found later by review recorded
afterwards, if at all.

**Where.** `openspec/config.yaml` (project context and rules) and
`openspec/schemas/gamestack/` (the fork of `spec-driven`), which `openspec
update` leaves alone; `.claude/skills/openspec-*/`, which it rewrites, so they
are never edited by hand. The CLI is pinned by hand, the version the Skills'
`generatedBy` names: `npm install -g @fission-ai/openspec@<version>`, or
`openspec install` for a standalone binary without Node. The profile is per
machine and set once:
`openspec config set profile custom`, `delivery skills` (no `/opsx:` commands
duplicating the Skills), `workflows '["propose","explore","apply","update","sync","archive","verify"]'`
and `telemetry.enabled false`, then `openspec update`.

**Pilot.** Detail's spec is the first change run through these Skills. The
owner then judges its output: assumptions visible rather than buried, every
scenario with a verifier, no rule in CLAUDE.md or `.claude/rules/` breached, and each Skill's steps still
there after a compaction. Kept, `discovery-feature` retires; dropped, the
format stays by hand and `discovery-feature` writes the change. The CLI stays
only if `validate` or the archive merge caught something a hand edit would not.

**Ruled out.**
- *Spec Kit.* Eight files per feature, a `constitution.md` that would be a
  second CLAUDE.md, numbered branches against ADR 0034, and a Python toolchain
  for one step.
- *Kiro and Tessl.* Kiro is its own IDE; Tessl makes the spec the source and
  was in beta. Kiro's WHEN/SHALL form and its bugfix shape were kept.
- *Editing a generated Skill.* `openspec update` overwrites it; the project's
  specifics go to the schema and the config instead.
- *Converting every screen at once.* A capability gets its spec from its first
  change; Search was written as the worked example.

## 14. Session handoff
**What.** A SessionStart hook that injects two small files kept outside the
repository — `HANDOFF.md` (the session's state) and `CYCLE.md` (the open
cycle) — and facts it derives live: the local branches, the next ADR number,
the issue pool and whether a triage is owed. It warns when the handoff is over
its bound or holds a section the Skill does not allow, and names the plans
beside it that neither file cites, ten at most. It deletes a branch's folder
under `reviews/` and `captures/` once the branch is neither local nor on a
remote, and names what it deleted; it warns about a record outside such a
folder, which it never deletes (ADR 0067). The `checkpoint` Skill holds the rules both
files follow, what else is kept beside them, and the steps to rewrite them
(ADRs 0045 and 0065). A line of its output is paid by every session, so it
prints only what a session acts on at its start and Claude Code does not
inject already.

**The next session's model.** The checkpoint's closing report names the model and
effort for the next action, read from a table of kinds of session in the Skill, and
the owner sets them when that session starts, since a switch mid-session rewrites
the prompt cache. The rows are checked once, by a pilot at one triage (#214), and
stay as they are after it (ADR 0076).

**Replaces.** A 64 KB append-only handoff that every session re-read, mixing
state with rules, design and history, reached through an agent-memory pointer
that was itself stale.

**Where.** `.claude/hooks/session-start.sh`, registered in
`.claude/settings.json`, and `test-session-start.sh` beside it, its cases, run
by `commits.yml`; `.claude/skills/checkpoint/`. The files are read from
`$CLAUDE_HANDOFF_DIR`, or `~/Documents/<main checkout's folder name>`.

**Ruled out.**
- *The handoff inside the repository.* Every pull request diff would carry the
  state churn of the session that opened it.
- *The rules as each file's header.* The hook paid for them on every session,
  and a clone never saw them.
- *A Next list in the handoff.* What comes next is the cycle; the rest is an
  issue (ADR 0038).
- *Stored derived facts.* Branches, hashes and counts went stale between
  sessions; the hook computes them instead.
- *A handoff section for constraints written nowhere else.* It filled with
  rules, lessons and decisions that no clone read, around the step that routes
  each to its home (ADR 0065).
- *`git status` and `git log` in the hook.* Claude Code's own `gitStatus`
  already holds both at a session's start.
- *Listing every open defect at the start.* No session acted on the list; a
  defect enters the work through the triage, which reads the pool itself.
- *A triage owed by counting merges* (ADR 0038, until ADR 0047). A one-line
  `.gitignore` merge made it fire mid-cycle, and a product cycle would owe one
  per pull request; skipping merges by path needed a hand-kept list.
- *Deleting a branch's records in a `merge-pr` or `checkpoint` step.* Auto mode's
  classifier refuses an agent's `rm` in that folder (§ 10), and a merge made on
  GitHub runs no step; the rule written with no step to carry it out left the
  records in place.
- *The hook deleting plans.* Whether a plan can go is a judgement on what it
  still holds, so the owner runs the command the checkpoint gives.
- *A log of each session's tokens for the triage to tune the model table.* At one
  person's volume the task set the cost far more than the model, so it could not
  confirm a row for many cycles. Claude Code's own `cost-state` record was no
  shortcut: it carries over a `/clear` and resets on a resume (ADR 0076).

**Known limit.** The cycle is one machine's file, so on a team nobody else sees
it. With a second person or machine keeping cycles, each cycle is a milestone:
its description holds the goal, the exit, each item's clause and the
insertions, its issues are the items, and closing it archives it. Not built
while one owner works on one machine: every status change would be a GitHub
write, and every session a `gh` read, with no other reader. The candidates
weighed are in the `gated` issue #159.

## 15. Agent attribution
**What.** The agent appears as a commit co-author only. `pr` is empty, so no
"Generated with Claude Code" line enters a PR body; `sessionUrl` is off, so a
cloud or Remote Control session adds no `Claude-Session` trailer or link, the
only sessions it acts on; and `commit` stays unset, so the `Co-Authored-By:`
trailer names the model in use when the commit is made. A personal
`.claude/settings.local.json` that sets `attribution` takes precedence for its
owner, unlike a `deny` rule; whether the object merges key by key is
undocumented.

**Replaces.** Attribution lines repeated in every PR body and a session link no
reader outside the session could open.

**Where.** `attribution` in `.claude/settings.json`.

**Ruled out.**
- *Setting it in user settings only.* A clone would get Claude Code's defaults.
- *A fixed `commit` string.* It would name one model forever, whichever model
  made the commit.

## 16. Agent memory gate
**What.** A `PreToolUse` hook on Write and Edit that denies the first write to
each agent memory file in a session, its reason the question ADR 0066 asks:
would this serve a clone or another person, and then go to the repository; does
it serve nobody, and then not be written. The same call sent again goes through.
The index of memories is let through, since it only points at them.

**Replaces.** Lessons that gathered in one machine's memory unasked, found only
by a walk through every file weeks later (#88).

**Where.** `.claude/hooks/memory-gate.mjs`, registered in
`.claude/settings.json`, and `test-memory-gate.sh` beside it, run by
`commits.yml`. It keeps which files it asked about per session in the system's
temporary folder, or `$CLAUDE_MEMORY_GATE_DIR`, for two days.

**Ruled out.**
- *Context added without blocking.* It reaches the model beside the tool's
  result, after the file is written, so a memory that served nobody would need
  deleting, which auto mode's classifier refuses.
- *An `ask` decision.* The owner would answer a permission prompt on every
  memory write, where the agent can answer the question itself.
- *Parsing the input in shell.* A memory's content can hold a `file_path` of its
  own, and Git Bash has no `jq`.
- *Matching shell commands too.* A command names the memory folder in more
  spellings than a pattern reads (§ 10), so a memory written through the shell
  is not asked about.

---

## Running on Windows
Not an entry: nothing in the repository configures it. What a session on
Windows with Git Bash runs into:
- A script passed inline through the agent's shell tool can lose its
  backslashes, a `\n` arriving as a newline: write it with the Write tool and
  run the file.
- Git Bash has no `jq`: use `gh --jq`, node, or sed and awk.
- `python` is the Microsoft Store stub; `py` is the real interpreter.
- A worktree under a long path fails `git worktree remove` with a filename too
  long in `build/`: stop its Gradle daemon, `rm -rf` the folder, then
  `git worktree prune`.

## Rebuilding this elsewhere
The copy generalised for export (issue #68) is made when a second project takes
the harness, not before. Order matters only where one piece depends on another:

1. **Version catalog and the Gradle pins** first — everything else assumes a
   build that behaves the same on two machines.
2. **The governing documents** next, even if thin. Rules written after the code
   are rules argued against the code.
3. **The gate workflow** and **`commits.yml`** (entry 11), and let each pass once.
4. **Branch protection** only then, requiring both checks, for the reason in
   entry 2.
5. **The PR template**, **the issue form**, and **Skills** as procedures prove
   themselves repeatable — a Skill written before the second run of a task is a
   guess.
6. **With more than one person or machine, give the cycle its shared home
   before the first cycle**: entry 14's known limit, and #159 for its shape.
7. **Choose where reviews run before the first one**: local rounds for one person
   with an agent session, a reviewer on the PR when several people or contributors
   without one read the findings (entry 8).

If this section ever grows past ordering into actual steps, it has become a
procedure and belongs in a Skill, not here.
