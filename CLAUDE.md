# CLAUDE.md — GameStack

## What is this project
GameStack is a native Android app for exploring video games using the IGDB API.
Users can search, view game details, manage personal lists, and rate games.
Not a store or gaming platform — a catalog and personal library app.

Full product scope, MVP feature list, and backlog live in
`docs/project/GameStack-Spec-v1.md` — consult it for product-level questions
(what should exist, what's deferred). This document (CLAUDE.md) governs
technical/architectural rules only.

## How to read this document — Three-Tier Boundary System
Every rule below belongs to one of three tiers:

- **Tier 1 — Immutable.** Breaking this rule either breaks the system mechanically,
  or contradicts a deliberate foundational decision. Never deviate without an
  explicit conversation first.
- **Tier 2 — Configurable within a defined range.** A bounded exception already
  exists and is listed. Anything outside that stated bound still requires asking first.
- **Tier 3 — Suggested.** Can be challenged with a stated justification. Propose
  the alternative and the reason — don't substitute silently.

## Documentation completeness rule
If this document (or a Skill) references a component that doesn't fully
exist or isn't fully specified yet, that reference must either be fully
specified in the same edit, or explicitly marked `(PENDING — not yet
defined)`. A vague reference that looks complete is worse than an honest
gap — it hides the missing piece until an agent trips over it mid-task.
(This is the exact pattern that happened with AuthRepository — referenced
in this document before its behavior was written down anywhere. Now fixed;
the rule exists to catch the next case, whatever it turns out to be.)

## Human-in-the-Loop principle

### Tier 1 — Immutable
Decisions split into two kinds, and they are handled differently:

- **Plan-level decisions** — anything requiring judgment that isn't mechanically
  derivable from this document, the Spec, or DESIGN.md: visual/UX choices,
  product scope, naming of a user-facing concept, or any trade-off with no
  single correct answer. **The agent proposes, the human decides.** Stop and
  wait for explicit approval; do not proceed on a "reasonable default".
- **Implement-level decisions** — anything already determined by the documents
  or by an existing pattern in the codebase. The agent proceeds without asking.

When a step is Plan-level, the Skill that owns it says so explicitly and names
the checkpoint (e.g. `new-feature` step 5, the screen prototype). Running such a
Skill in Claude Code's Plan Mode enforces the pause mechanically (read-only until
approved) rather than relying on the agent to honor it.

Related but distinct: the Tier system governs *rules* (what may be changed and
who must agree). This principle governs *steps in a procedure* (when to stop and
ask mid-task). A Tier 1 rule can only be changed by conversation; a Plan-level
step must be approved every single time it runs.

## Self-Healing Loop

### Tier 1 — Immutable
When code and documentation disagree, that mismatch is **drift**, and drift is
never resolved silently. Resolve it in one of exactly two directions, and state
which one was chosen:

1. **Fix the code** — the document was right; the code deviated.
2. **Fix the document** — the code turned out to be right and the document is
   what's outdated (e.g. an implementation decision was made that the document
   never recorded).

Either way, log it in `docs/project/DRIFT-CHECKLIST.md`'s Resolution log.
Never leave a known contradiction unresolved on the grounds that "the code works" —
an undocumented decision is invisible to the next agent, which is how the same
drift returns.

## Commands
- Build: `./gradlew build`
- Unit tests: `./gradlew test`
  - Single class: `./gradlew testDebugUnitTest --tests "com.gamestack.Foo"`.
    Plain `test` is a lifecycle task and rejects `--tests`.
- Lint: `./gradlew lint`

### Device verification — Tier 2
The gate above (`build`/`test`/`lint`) is necessary but **not sufficient for UI
work**: it proves the code compiles and that the logic does what a test asserts,
never that a screen looks right or that a control is reachable. Every feature
that adds or changes a screen is also verified on the emulator before a merge is
requested, and the PR body says what was checked. Data/domain work with no UI
change does not need this pass.

- Install: `ANDROID_SERIAL=emulator-5554 ./gradlew installDebug`. `adb` is not
  on PATH — use `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`.
- **If no emulator is running, ask for one** — do not declare a UI feature done
  without this pass, and do not launch an AVD unprompted (it opens a window on
  the user's desktop).
- **Two tools, two questions.** `Grep` over `uiautomator dump` for assertions
  that can be stated in advance (which tab is `selected="true"`, whether a text
  or node exists) — only matching lines enter context, so it is by far the
  cheaper path. `adb exec-out screencap -p`, read as an image, when the
  criterion is visual (clipping, spacing, contrast, alignment) and **whenever a
  keyboard may be on screen**: the dump contains only the app window, never the
  IME, so it reports as visible controls the keyboard is actually covering.
  This cost real debugging time once — taps aimed from dump coordinates landed
  on keyboard keys. `dumpsys input_method` is unreliable on this AVD for the
  same question. Close every UI verification with at least one screenshot: the
  dump cannot find what nobody thought to assert, and three of the Search bugs
  were purely visual.
- Cover every state the screen declares (initial, loading, content, empty,
  error), plus the keyboard raised if there is a text field. Network-dependent
  states can be forced with `adb shell svc data disable` / `svc wifi disable`.
- Screenshot cost is set by aspect ratio, not by AVD resolution — see
  `docs/project/design/README.md`. Do not shrink the AVD to save tokens; it
  saves none.

### Accessibility — Tier 2
**MVP does not target accessibility.** TalkBack passes, large-font and contrast
audits, and touch-target sizing are backlog (Spec → Explicitly Deferred). So an
accessibility finding from a review is **non-blocking by default**: open an
issue, link it in the thread, merge. Do not build accessibility work that was
not asked for, and do not hold a PR for it.

Two things this does *not* license, because both cost far more to retrofit than
to keep:

- **Never drop an affordance that already worked.** Material components carry
  semantics for free — a `Snackbar` announces itself, an `IconButton` exposes a
  focus target. Replacing one with a hand-built equivalent silently loses that,
  and the loss is invisible to `build`/`test`/`lint` and to a screenshot. If a
  change removes an announcement or a focus target, restore it in that same
  change; that is *not* new accessibility work, it is not regressing.
- **This deferral has an expiry.** The app is headed for the Play Store, which
  publishes accessibility expectations and where retrofitting semantics across a
  finished UI is a rewrite. Revisit before the first public release, not after.

## Git workflow

### Tier 1 — Immutable
- **Always ask before `git push`, and before opening a PR.** Commits are local
  and reversible; a push is not, and a PR is outward-facing.
- **Never `--no-verify`.**
- **Never force-push `main`, nor any branch another person may have pulled, and
  never bare `--force` anywhere.** `--force` overwrites whatever is on the remote
  without checking. A bounded exception for cleaning your own unreviewed feature
  branch is defined in Tier 2 below; nothing outside that exception is allowed.
- **The gate is all three green:** `./gradlew build`, `./gradlew test`,
  `./gradlew lint`. A lint *Error* fails the gate; warnings do not. `git commit`
  freely once it passes.
- **Never commit directly to `main`** — branch first.
- **Code and its documentation land in the same commit.** A commit that changes
  an architectural rule carries its CLAUDE.md/Spec/DRIFT-CHECKLIST update with
  it (Documentation completeness rule). Never code now, docs next commit.
- **Read `git status` in full before committing.** Untracked source files are
  invisible to `git add -u` and silently break the build for everyone else.
- **Before pushing, `git fetch` and reconcile with `origin/main`.** Rebase or
  merge if the branch is behind. If local `main` is ahead of `origin/main`,
  push it first, or the PR will carry commits that have nothing to do with it.
  Divergence in either direction is a problem — check both, not just
  "am I behind".

### Tier 2 — Configurable within a defined range
- Branch naming: `feature/{name}`, `fix/{name}`, `chore/{name}`, `docs/{name}`.
- One branch per unit of work. A refactor that exists *only because of* a feature
  (e.g. navigation restructured so Detail belongs to a tab) belongs on that
  feature's branch — it cannot be built or tested independently of it.
- **Before each commit, ask whether it exists *because of* this branch's unit of
  work.** If it does not, it belongs on its own branch off `main` — park it with
  `git stash`, or `git cherry-pick` it onto a fresh branch afterwards. This rule
  already existed and PR #3 still reached 1,992 additions across 46 files, of
  which only two commits were the Search feature; the rest were an unrelated auth
  fix, two `.gitignore` chores, a design asset and three documentation-policy
  changes that merely happened to occur while the branch was open. What inflates
  a PR is how long the branch stays open, not how hard the feature is.
- Treat **400 changed lines or 15 files** as a smoke alarm, not a limit: past
  roughly that size review quality drops sharply, so stop and check the PR is
  still one reviewable claim. If its title needs an "and", split it.
- Commit messages follow **Conventional Commits**: `type(scope): imperative
  summary`, lowercase, no trailing period. Types in use: `feat`, `fix`,
  `refactor`, `docs`, `chore`, `test`. Scope is the feature or layer
  (`search`, `auth`, `nav`) and is optional for repo-wide changes.
  Example: `feat(search): search IGDB games with results, empty and error states`.
  (Commits before 2026-08-22 predate this convention — do not rewrite them.)
- Open a PR when a branch is ready for review, using
  `.github/pull_request_template.md`. The body states what changed, why, and how
  it was verified (gate output, emulator/device check). UI changes include
  screenshots: generate them locally, then **drag or paste them into the PR
  body's edit box on the web** — GitHub uploads them to its own CDN. Never commit
  emulator captures to the repo; only design references under
  `docs/project/design/` are versioned, and those can be linked by URL.
- **Never overwrite a PR body wholesale once it is open.** `gh pr edit --body`
  replaces everything, including screenshots the human dragged in and any text
  they added — GitHub's own edit history is then the only way back. Read the
  current body first and change only what needs changing, or add a comment
  instead. This already destroyed an uploaded screenshot on PR #5, minutes after
  asking for it.
- **Hand the captures over; do not make the human retake them.** The device pass
  already produced screenshots. Save them somewhere stable outside the repo
  (`~/Documents/GameStack/screenshots/pr-{n}/`, alongside the other out-of-repo
  artifacts for this project — named for what they show; a scratchpad or
  `%TEMP%` path gets cleaned), give the paths in chat when the PR is opened, and
  say plainly that the PR needs them dragged in **before** it is merged. An agent
  that verified on device and then let the human go hunting for a screenshot
  wasted the one part of this only the human can finish.
- **Automated review threads (Codex bot, `/code-review`) are closed explicitly.**
  Fix the finding or dismiss it with a reason, reply in the thread saying what
  was done, then **Resolve conversation** — the reply is the history, the resolve
  is only the filing. An unanswered thread is indistinguishable from an unnoticed
  one. Check the reviewed commit before trusting a bot review: it pins to the SHA
  it ran on, so a review from four commits ago may already be stale. Re-request
  with `@codex review` after pushing fixes.
- Run `/code-review` on any PR touching app code before asking for a merge;
  doc-only or chore-only PRs may skip it. Every finding is either fixed or
  dismissed with a stated reason in the PR thread — a review whose findings are
  silently dropped is worse than none, because it looks like coverage. If a
  finding exposes code/doc drift, log it in DRIFT-CHECKLIST's Resolution log per
  the Self-Healing Loop. The gate is mechanical and the review is semantic: on
  PR #3 the gate passed while six real defects were still in the diff.
- **Triage every finding into one of two buckets, and say which.**
  - *Blocking* — correctness, data loss, a defect the user can see. Fix before
    merge, with a regression test verified per Testing Stack.
  - *Non-blocking* — design, performance, polish, debt. Open an issue, link it in
    the thread, and merge. "Dismissed with a reason" is what lets a PR close;
    it is legitimate precisely because the reason is written down and linked.

  Taken as "fix everything a reviewer mentions", this rule never lets a PR land:
  an automated reviewer almost always finds *something*. **Stopping rule:** a
  round that produces only design or style suggestions is the signal to merge. A
  round that produces a real defect earns another round.
- **`/code-review` is run by the human — the agent cannot invoke it.** So the
  agent's job is to *ask for it and wait*, never to tick that box or excuse it.
  Do not confuse it with the GitHub bot: they are different reviewers, and the
  bot's quota says nothing about whether `/code-review` has run. That exact
  confusion was written into PR #5's body to justify skipping it.
- **Automated review is an advisor, not a gatekeeper.** This applies to the
  GitHub bot, not to `/code-review`. The blocking gate is
  `build`/`test`/`lint` plus `/code-review`. If the bot is unavailable — quota
  exhausted, service down — merge anyway; an external quota must not decide
  whether work ships. Request it **once per PR, when the PR is ready**, not after
  every fix; PR #3 burned five reviews in one day and then hit the limit. When
  waiting on one, watch for its usage-limit comment and stop waiting the moment
  it appears, rather than polling until the timeout.
- **Rule of three.** Touching the same code a third time for the same class of
  defect means the design is the problem, not the instances — stop patching and
  redesign. A fix that introduces the next defect in the same place counts double.
  Search's debounce logic took four rounds before this was acted on; the rewrite
  that followed deleted the state all four defects came from.
- **Merging: `Create a merge commit` is the default.** The branch's commits are
  curated (Conventional Commits, code and docs together) and are the unit that
  makes `git blame` and `git bisect` useful; the merge commit also records what
  landed together as one reviewed unit. Delete the branch afterwards.
  - `Squash and merge` is the fallback for a branch whose history is genuinely
    disposable — not the default. Squashing keeps the *text* of the messages (as
    bullets in the body) and destroys the structure: per-commit SHAs, line-level
    `blame`, and every bisection point but one. It would also make two rules here
    pointless — writing careful Conventional Commit messages that are discarded
    at merge, and "code and its documentation land in the same commit", which is
    tautological once everything is one commit. If it is used, the subject must
    still follow Conventional Commits; GitHub defaults it to the PR title.
  - `Rebase and merge`: do not use. Replaying commits onto a new base produces
    intermediate states that never existed and never passed the gate, so
    `git bisect` can land on a commit that does not build, and the grouping of
    what was reviewed together is lost.
- **A branch is clean when** every commit message names a change worth finding
  later (no `wip`, `fix`, `address review`); every commit passes the gate on its
  own; no commit exists only to correct an earlier commit *on this same branch*
  (that is a fixup — fold it in); and nothing is added and then removed within
  the branch. Clean it with `git commit --amend` or `git reset --soft` and
  recommit; `git rebase -i` is unavailable in this environment.
- **Force-push is allowed to clean your own feature branch, and only until it has
  been reviewed.** Use `git push --force-with-lease`, never bare `--force`:
  with-lease refuses to write if the remote moved since your last `fetch`, so it
  cannot silently overwrite anything you have not seen.
  - **The window closes at the first review, not at the first push.** Once the
    Codex bot or `/code-review` has commented, rewriting commits breaks the
    anchor for those threads — they go outdated or hang off a SHA that no longer
    exists. That review history is worth more than a tidy log: on PR #3 it is
    what caught two defects that the gate passed.
  - After that point the only remedy for a messy branch is `Squash and merge`,
    with the cost described above. Cleaning early is what keeps the merge commit
    (and with it `blame` and `bisect`) as the default.
  - This is deliberately a conditional rule where an absolute one would be easier
    to obey. It is accepted because the risk the absolute version guards against
    — overwriting work someone else holds — does not exist in a solo repo, while
    its cost (losing a whole branch's granularity over one clumsy commit) is
    permanent and recurring. The condition is cheap to check: does the PR have
    review comments yet?

### Tier 3 — Suggested
- Commit as you go, not in one blob at the end. A feature that reaches completion
  entirely uncommitted forces a choice between one unreviewable commit and
  inventing a history that never existed.
- Keep unrelated housekeeping (IDE files, ignore rules, asset corrections) in
  their own commits so a feature's history stays readable.

---

## Tech Stack

### Tier 1 — Immutable
- Kotlin (latest stable) — project language.
- Jetpack Compose — all UI, no XML, no ViewBinding.
- MVI + Clean Architecture — governs every structural rule in this document.
- Retrofit + Kotlinx.serialization — networking (see Data Sources for the
  Apicalypse-specific constraint).
- Navigation Compose with Safe-type routes.
- Material3 — Dark theme only for MVP. Light theme is backlog (see
  Pending/Roadmap) — do not build a Light ColorScheme now.
- **No dynamic color (Material You wallpaper-based theming).** The palette is
  the fixed one in DESIGN.md. Dynamic color would let the OS override the
  violet that carries the "Stack" brand identity, and would make DESIGN.md
  non-authoritative for what actually renders. `GameStackTheme` therefore takes
  no `dynamicColor` parameter and calls no `dynamicDarkColorScheme()`.
- Before adding ANY new library (not just replacing one below): check for the
  latest stable version first — never assume a remembered version is current.

### Tier 2 — Configurable within a defined range
- Room — local persistence. Swappable in theory (e.g. a future SQLDelight
  migration for KMP), but touches the Data layer contract broadly — ask first.
- Hilt — dependency injection. Same reasoning as Room: high blast radius, ask first.

### Tier 3 — Suggested, can be challenged with justification
- Coil — image loading. If there's a concrete reason (deprecation, critical bug,
  better Compose support elsewhere), propose an alternative and explain why.

---

## Testing Stack

### Tier 1 — Immutable
- Turbine, MockK, TestDispatcher — baked into the `write-tests` skill's patterns.
  Changing any of these breaks consistency across every test in the project.
- Use the project's `MainDispatcherRule` (`core/testing/`) for ViewModel tests
  instead of manually managing `Dispatchers.setMain()`/`resetMain()`.
- **Everything under `test/` is a plain JVM unit test** — no Robolectric, no
  emulator, no real database or network. `./gradlew test` is the gate, and it
  must stay fast enough to run on every change. Anything genuinely requiring
  the Android runtime (a real Room DAO against SQLite, Compose UI interaction)
  is an *instrumented* test under `androidTest/` — currently backlog, not MVP.
  Consequence: DAO interfaces are verified indirectly, through Repository tests
  with a mocked DAO. See `write-tests` for what that does and does not prove.
- **Every regression test must be seen to fail against the code it guards.** A
  test written after the fix can pass whether or not the fix is there, and that
  test is worse than none: it looks like coverage and proves nothing. Park the
  production change (`git stash push -- <file>`), run that test alone, confirm it
  fails *and that the message describes the bug* — a failure from a compile error
  or an NPE is red for the wrong reason — then restore. Doing it the other way
  round (test first, watch it fail, then fix) gets this for free. **When the fix
  is a redesign the old code no longer compiles against, stashing cannot work** —
  it would fail for the wrong reason. Break the fix instead: mutate one line the
  new test claims to cover, and confirm exactly that test fails. State in the
  commit which method was used; nine defects in Search were found by review after
  the gate passed green, so "the tests pass" is not by itself evidence of
  anything. See `write-tests` for the recipe.

See the `write-tests` skill for full conventions (setup per layer, naming, Happy/Sad paths).

---

## Architecture

MVI with Clean Architecture. Three logical layers (UI, Domain, Data) as packages
within a single app module.

### Tier 1 — Immutable
- UI never accesses the Repository directly — always via UseCase through the ViewModel.
- Repositories contain no business logic — they only coordinate data sources.
- **Never build a `Result` with `runCatching` around a suspend call.** It catches
  `Throwable`, `CancellationException` included, so a coroutine that was merely
  cancelled (a superseded debounce, a ViewModel clearing) comes back as
  `Result.failure` and surfaces to the user as a real error. Use `try`/`catch`
  that rethrows `CancellationException` before catching `Exception`. This is not
  a style preference: it produced a visible bug where every cancelled Search
  flashed the error screen before the next results landed.
- Domain models never import Retrofit, Room, or Android types (only allowed
  external dependency: `javax.inject` for Hilt).
- Never use LiveData — only StateFlow and Flow.
- Composables hold no logic except view logic.
- **UI layer:** Composables + ViewModels (MVI pattern — see `new-viewmodel` and `new-screen` skills).
- **Domain layer:** UseCases and pure models.
- **Data layer:** Repositories, DAOs, API Services, Mappers.
- **Navigation:** persistent bottom navigation bar with exactly 3 top-level
  destinations — Home, Search, Library — following Material 3 guidelines
  (3-5 destinations of equal importance, reachable from anywhere). Detail is
  NOT a bottom nav destination — it's reached from a game card in any of
  the three, not a top-level tab. High cost to change once built — ask first.
- **Navigation structure — one nested graph per tab.** Each tab is a nested
  graph (`HomeGraph`/`SearchGraph`/`LibraryGraph`) whose start destination is
  that tab's screen, and `Detail` is registered *inside every tab graph* via the
  shared `NavGraphBuilder.gameDetailDestination()` helper — never once at top
  level. That is what gives each tab its own back stack. It does not add a
  fourth tab: the graphs are structure, the tabs are still exactly three.
  Consequences, all intended:
  - A Detail belongs to the tab that opened it: that tab stays selected while
    Detail is on screen, and Back from Detail returns to that tab's own screen.
  - Switching tabs saves the originating tab's whole stack (Detail included) and
    restores it on return — coming back to a tab lands you exactly where you
    left it, **including on an open Detail**. Returning to a tab deliberately
    does not reset it to its root.
  - Back from a non-start tab goes to Home, then exits the app. This is standard
    Android bottom nav behavior, not a defect — do not "fix" it.
  - Bottom nav items and `navigateToBottomNavDestination()` always target a
    `*Graph` route, never the screen inside it; targeting the screen stops the
    tab's back stack from being the unit that gets saved and restored.
  - Never use `NavController.currentBackStack` — it is `@RestrictTo`
    library-group API and fails the `lint` gate. If tab switching appears to
    need it, the graph structure is wrong, not the API.

  Registering `Detail` once at top level instead is what previously let it leak
  across tabs — it would reappear under an unrelated tab with no tab selected.

### MVI Contract conventions (Tier 1)
Every screen defines exactly three contract types, and they are the only way
UI and ViewModel communicate:

- `{ScreenName}UiState` — a data class, exposed as `StateFlow`, backed by a
  private `MutableStateFlow`. Use a sealed class only when the states are
  genuinely mutually exclusive; a data class with nullable/boolean fields is
  the default, because loading, content, and error frequently coexist (e.g.
  a refresh failing while stale content is still on screen).
- `{ScreenName}UiEvent` — a sealed class covering every action the UI can send.
- `{ScreenName}UiEffect` — a sealed class of one-shot events (navigation,
  snackbars), exposed via `Channel` + `receiveAsFlow()`. Never put a one-shot
  event in UiState: state replays on recomposition/rotation, so a navigation
  or snackbar modeled as state fires twice.
  The `Channel` **queues** anything sent while the screen is not collecting —
  that is deliberate (a snackbar raised during a config change must survive),
  and it is why the alternative, `SharedFlow(replay = 0)`, is not used here:
  it would silently drop those instead. The cost of queueing is that a
  *duplicated* effect is not merely handled twice, it is replayed when the user
  returns to the screen. So the invariant is at the source: **one user intent
  must emit exactly one effect.** Wrap **every** callback that emits a
  navigation effect in `rememberSingleClick` (`core/presentation/`) — a list's
  items, an empty-state CTA, a toolbar action, all of them. For a list, share one
  wrapper across the whole list so two different items cannot fire back to back;
  a no-argument overload exists for single controls. Auditing this is per screen,
  not per list: the CTA on Search kept emitting duplicates because the wrapper
  was introduced for the result cards only, which is where the bug had been seen.
  Do not "fix" a duplicated effect by changing the Channel — that trades a
  visible bug for a silent one.

All three live together in `feature/{name}/presentation/{ScreenName}Contract.kt`
— this is the one-class-per-file exception (1) below. The ViewModel exposes a
single public entry point, `fun handleEvent(event: UiEvent)`.

### Tier 2 — Configurable within a defined range
- Feature-based package structure — the *principle* of feature-based
  organization is fixed, but exact folder names can evolve. Ask first: every
  Skill's "Location" section references these paths. The full set:
  - `feature/home/`, `feature/search/`, `feature/library/`, `feature/detail/`
    — one per screen, each with its own `domain/`, `data/`, `presentation/`
    subpackages as needed.
  - `core/domain/`, `core/data/` — shared across two or more features.
  - `core/presentation/` — shared UI-layer utilities that are not screens
    (e.g. `UiText`).
  - `core/testing/` — shared test utilities (e.g. `MainDispatcherRule`),
    in the **test** source set, not main.
  - `ui/theme/` — Theme, Color, Type, Shape. App-wide, not feature-scoped.
  - `navigation/` — NavHost, type-safe `Destination` routes, bottom nav bar.
- Pull-to-refresh is expected on any screen displaying a list/collection
  (Home, Search results, Library). Single-item screens (e.g. Detail) don't
  need it. See the `new-screen` skill for the implementation pattern.

---

## Data Sources

### Tier 1 — Immutable
- **IGDB API:** read-only. Requires OAuth via Twitch — see AuthRepository.
  `AuthRepository` uses Twitch's Client Credentials flow (app-level auth,
  no user login involved) — a POST to `id.twitch.tv/oauth2/token` with
  client ID/secret. It caches the token in memory, guarded by a `Mutex`
  to prevent duplicate concurrent fetches, and auto-refreshes it before
  expiration (with a safety buffer). An `AuthInterceptor` attaches **both**
  required headers — `Client-ID` and `Authorization: Bearer {token}` — to
  every IGDB request automatically. Never fetch or attach them manually:
  not in a UseCase/ViewModel, and not as `@Header` parameters on the
  Retrofit interface either (see `new-api-service`).
  If the token cannot be obtained, the interceptor throws `IOException`
  and the request fails — it does **not** proceed unauthenticated. An
  unauthenticated IGDB call would fail at the server with a 401 anyway;
  failing early keeps the error at one layer instead of two. Retrofit
  surfaces this to the Repository as a normal network failure, so it flows
  into the existing `Result` error path with no special handling.
  The Twitch auth client is deliberately built **without** this interceptor —
  the call that fetches the token cannot depend on already having one.
  IMPORTANT: IGDB's own data endpoints (games, genres, etc.) do NOT use
  standard REST — they use Apicalypse: every request is a POST with the
  query as plain text in the body, not GET with query params. See the
  `new-api-service` skill before touching that layer. This does NOT apply
  to the Twitch auth call above, which uses standard REST.
- Personal ratings and lists are never synced with IGDB's own community rating —
  they are independent, local-only data.

### Tier 2 — Configurable within a defined range
- **Room (local):** a single `UserGameEntity` per game the user interacted with —
  `gameId` (PK), `listStatus` (nullable: PLAYING_NOW/WANT_TO_PLAY/COMPLETED),
  `rating` (nullable: `GameRating.LIKED`/`DISLIKED`), `updatedAt` (any change —
  powers "Recently Interacted" on Home), `completedAt` (nullable — set ONLY
  when `listStatus` transitions to COMPLETED, never touched by rating changes;
  powers "Completed on [date]" in Library). Do not conflate `updatedAt` and
  `completedAt` — they answer different questions. Do not split lists and
  ratings into separate tables — a game can have either, both, or neither
  independently. Never synced with IGDB's own community rating.

- **Who owns the `updatedAt`/`completedAt` write rules: the Repository.**
  Both are *persistence invariants* — statements about how a row is written —
  not domain rules, and they are explicitly permitted inside `UserGameRepositoryImpl`
  despite the "Repositories contain no business logic" rule. The mechanics:
  every write is read-modify-write (load the existing row, produce the updated
  copy, upsert), `updatedAt` is stamped on every write, and `completedAt` is
  stamped only when the incoming `listStatus` is COMPLETED and the stored one
  was not. Detecting that transition requires comparing against the stored row,
  which only the Repository can see.

  This is deliberately *not* a UseCase's job: a UseCase would have to read the
  current entity to compare, and entities never leave the Data layer. It is
  deliberately not the DAO's job either, since it spans a read and a write.
  The boundary that still holds: the Repository decides *when a timestamp
  column is written*, never *what the timestamps mean to the user* — anything
  user-facing (formatting "Completed on [date]", ordering "Recently Interacted")
  stays in the Domain/UI layers. If a future rule needs more than comparing
  old and new persisted state, that is real business logic and belongs in a
  UseCase — ask first.

---

## Code Conventions

### Tier 1 — Immutable
- PascalCase for classes, camelCase for functions — Kotlin ecosystem-wide convention.
- Mappers must live in the data layer. The rule they enforce is directional:
  **a DTO or Entity type must never appear in a signature outside the data layer.**
  Two mapping directions exist, and both are legitimate:
  - **Read path — `DtoOrEntity.toDomain()`**: converts inbound data into a Domain
    model. This is the common case and the only one `new-mapper` used to cover.
  - **Write path — `DomainModel.toEntity()`**: converts a Domain model into a Room
    Entity so it can be persisted. Its *input* is a Domain model and its *output*
    never leaves the Data layer, so the boundary holds. Needed by every write
    feature (saving a rating, moving a game between lists) — without it, that
    conversion would have no defined home.

  What remains forbidden: a mapper that returns a DTO/Entity to a caller in the
  Domain or UI layer, and any DTO→Entity mapper (remote and local models must not
  know about each other — convert through the Domain model). Same-layer
  transformations (building an updated copy of an Entity to persist) are not
  Mappers at all — see `new-repository-impl`.
- All code and comments in English — foundational project decision.
- Never hardcode UI strings in ViewModels or UseCases — always use `UiText`
  (`UiText.StringResource` for translatable strings, `UiText.DynamicString`
  only for truly dynamic values from an API or database).

### Tier 2 — Configurable within a defined range
- One class per file, EXCEPT:
  (1) MVI contract classes (`UiState`, `UiEvent`, `UiEffect`) for the same
  screen live together in `{ScreenName}Contract.kt` — they only make sense as a
  group and are always edited together. This is the expected layout, not merely
  a permitted one; splitting them across three files is what needs justifying.
  (2) A small `private` helper type used exclusively by one Composable/class
  in that same file (e.g. a private data class configuring a static list of
  items for that Composable) — it has no meaning or reuse outside that context.

- **Comments carry the *why*, never the *what* and never the *history*.** Three
  questions before leaving one:
  1. Does it restate what the code already says? Delete it.
  2. Is it about *how we got here* rather than *what to do here*? That belongs in
     the commit message and DRIFT-CHECKLIST's Resolution log, both of which this
     project already maintains. Writing it in both places is duplication, and the
     copy in the code is the one nobody updates.
  3. Would it become false if someone refactored correctly? Then it is a
     chronicle, not a constraint — delete it or rewrite it as the constraint.

  Prefer encoding the *why* as a name, a type, or a **test**: a test named
  `should cancel an in-flight search immediately when the query changes` states
  the invariant *and fails when it breaks*, which no comment can do.

  **Bounds:** no comment block longer than 4 lines in production code — anything
  needing more belongs in the owning Skill with a one-line pointer — and treat
  **15% comment lines in a file** as a smoke alarm worth looking at, not a hard
  limit. Both numbers are heuristics; the three questions are the actual rule.
  The percentage is meaningless below ~50 lines — one justified 5-line comment
  puts a 30-line contract file over 15% — so read it as a signal on substantial
  files and judge short ones by the block bound and the questions alone.

  This is a known LLM failure mode, not a hypothetical: models narrate design
  decisions into comments, and the words are free to produce and costly to read.
  `SearchViewModel.kt` reached 25% and `SingleClick.kt` 35%, one sediment layer
  per review round, while every other file in the project sat between 5% and 9%.

### Tier 3 — Suggested, can be challenged with justification
- Mapper file naming pattern (`{Name}Mapper.kt`) — consistency preference,
  not a structural requirement.
- Avoid chaining more than 2 functional operators (`map`/`also`/`let`/etc.)
  in a single expression when the chain performs a side effect (e.g. a
  cache assignment) — prefer a named intermediate variable. Token cost of
  this is negligible; readability during future debugging matters more.
- Avoid duplicating non-trivial code (more than a couple of lines, or logic
  that could drift out of sync if only one copy gets updated) anywhere in
  the codebase — extract to a `private` function or named variable instead.
  Applies everywhere: ViewModels, UseCases, Repositories, Composables, Hilt
  modules, wherever the pattern appears — not scoped to any single layer.
- In Hilt modules specifically, beyond the duplication rule above: only
  expose a value via `@Provides`/`@Binds` if something outside this module
  genuinely needs to inject it directly; otherwise keep it private to avoid
  unnecessarily widening the DI graph. Exception: anything that must be
  `@Singleton` stays a binding even if only same-module consumers inject it —
  scope applies to bindings, not to functions, so a private helper would
  silently create one instance per call site. See `new-hilt-module`.
- If a dependency is genuinely needed by multiple modules, extract it to
  its own appropriately-named module (e.g. `SerializationModule` for
  `Json`) — Hilt's graph is flat, not hierarchical, so any module can
  consume it regardless of which file provides it. Avoid a catch-all
  "CommonModule" — name modules by what they represent.

---

## Available Skills
This project uses Claude Code Skills for repeatable procedures. Prefer invoking
these over improvising the same task differently each time:

**Setup / planning**
- `discovery-feature` — turns a vague feature idea into a Spec section. Only
  needed when scope is genuinely ambiguous; the MVP screens are already specced.

**Implementation (in dependency order)**
- `new-repository-interface` — Domain layer Repository contracts
- `new-repository-impl` — Data layer Repository implementation
- `new-api-service` — IGDB Retrofit interface (Apicalypse syntax)
- `new-room-dao` — Entity + DAO + TypeConverters + Database registration
- `new-hilt-module` — Hilt bindings for anything the above creates
- `new-mapper` — `toDomain()` / `toEntity()` conversions
- `new-usecase` — Domain layer UseCases
- `new-viewmodel` — MVI ViewModels
- `new-screen` — Composable screens
- `write-tests` — Unit tests for any of the above
- `new-feature` — orchestrates all of the above end-to-end

If a task doesn't map to any Skill above, that's a coverage gap worth naming:
do the work, then say so, so the gap can be closed deliberately rather than
by improvising the same task differently every time.

**Retired skills** — kept as a record, not invocable, not in `.claude/skills/`:
- `project-scaffold` — bootstrapped this project (deps, folder skeleton, Hilt,
  Theme, core test utilities). Ran once, at the start; retired 2026-08-22 because
  a once-per-project skill has nothing left to do here and only added a dead
  option to the listing. Archived at
  `docs/project/retired-skills/project-scaffold.md`.

## Pending / Roadmap

**About "Block N":** the numbering comes from the development phases this
project is being built in, and several documents reference it. Only the phases
actually referenced somewhere are defined — here and, in product terms, in the
Spec's "Roadmap Phases" section. Do not infer meaning for an undefined block
number; if a document references one that isn't defined in either place, that
is a documentation gap to report, not to guess at.

- **Block 4 — Loop Engineering** (harness work, not product): automate what is
  currently manual review. Two concrete deliverables are already referenced by
  other documents and both are still pending:
  1. A Hook that runs the affected test suite automatically after each agent
     edit — the Regression pillar that `write-tests` explicitly does not cover
     (it covers Requirements only: "new code does what the Skill/Spec asked").
  2. An automated drift checker (Hook or validator subagent) implementing
     `DRIFT-CHECKLIST.md`'s items, replacing the manual pass.
  3. Automating the regression-test verification in Testing Stack (Tier 1) —
     currently a manual `git stash` recipe in `write-tests`. Ranked above (2) by
     value: nine real defects in Search all passed a green gate, and this is the
     habit that caught whether each new test was worth anything. **Not a naive
     `PostToolUse` hook**: stashing and restoring a live working tree on every
     edit risks the user's uncommitted work, fires far too often, and cannot
     tell a regression test from a test for brand-new code. Likelier shapes: a
     script the workflow calls deliberately, or a `Stop` hook that *checks
     whether the verification was done* when a diff adds tests and changes
     production code, rather than performing it. When it lands, the Tier 1 rule
     stays — reword it from "do this" to "the hook checks this, here is what it
     checks and why". Deleting the rule would leave a script nobody can explain.
- **Block 5 — AI-powered recommendations** based on user history (Room).
- Future: KMP migration. Data layer prepared for it — Retrofit → Ktor,
  Room → SQLDelight, without touching Domain or UI.
- Future (backlog, not MVP): extend `GameRating` from binary (LIKED/DISLIKED)
  to a third "LOVED" state (Netflix/Prime-style double thumbs up).
  Trivial to add later since it's an enum — do not build now.
- Future (backlog, tooling): consider adopting OpenSpec as an active
  change-tracking layer alongside CLAUDE.md, IF the project grows beyond
  solo development (team context) or manual tracking via Skills becomes
  a real pain point. Not worth the added infrastructure at current scale.
- Future (backlog): Light theme ColorScheme. Project design identity
  (established via Stitch prototyping and GameStack Core DESIGN.md) is
  dark-first, matching gaming apps like Steam/Xbox/Discord. Not worth
  building a parallel Light ColorScheme now — revisit only if there's a
  real accessibility or user need.
