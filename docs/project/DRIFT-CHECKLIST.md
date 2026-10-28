# DRIFT-CHECKLIST — GameStack

> Manual audit checklist derived from CLAUDE.md's Tier 1 (Immutable) rules. It
> detects drift between the code and the Project Constitution, and it runs on
> either of two events. A pull request touches `CLAUDE.md`, any `.md` under
> `docs/project/` or `.github/`, any Skill under `.claude/skills/`, or
> `.claude/settings.json` — a diff
> shows you that outright — and what runs then is the *Documentation
> integrity* set, fired by `ship-a-branch`
> ([ADR 0022](decisions/0022-a-governance-diff-triggers-the-checklist.md)) and
> scoped by what the diff touches, as that section's own header says.
> Or a batch of agent-generated features lands, which no diff marks, so that
> half still depends on someone noticing; that is the event for the rest of the
> file, the detectors that read code.
>
> Each item is phrased as a verifiable question, and **every item is written so
> that "no" is the healthy answer** — a "yes" means drift was found. Read them as
> violation detectors, not as goals.
>
> When drift is found, resolve it per the Self-Healing Loop (CLAUDE.md): either
> fix the code, OR update the document if the code turned out to be right and the
> document is what's outdated. Record which direction was chosen as a `Drift:`
> trailer on the commit that resolves it (ADR 0033); a decision that is born or
> changes is also an ADR under `docs/project/decisions/`.
>
> Every item carries a **`Verified by:`** line naming what checks it *today*,
> not what could check it one day. The vocabulary is closed:
>
> - `human` — a person reads the code or the documents and decides. This is the
>   default, and it says nothing about whether the item *could* be mechanised:
>   most of these could be, and none of them are yet.
> - `subagent (interpretive)` — the question needs judgment to answer at all, so
>   no test or script will ever settle it on its own. A claim about the item's
>   nature, not about today's tooling.
> - `./gradlew lint`
> - `test: <TestName>#<caseName>` — spelled as `decisions/README.md` spells it,
>   so the two vocabularies can be joined where they overlap.
>
> That is what makes this file the coverage map for the automated checker in
> `docs/project/TOOLING-BACKLOG.md` (item 3) rather than just its wish list: the
> `human` lines are the work it can take over, the `subagent (interpretive)`
> lines are the work it cannot, and a line already naming a tool is work it does
> not need to. Read the map with
> `grep -o "^ *Verified by: .*" docs/project/DRIFT-CHECKLIST.md | sort | uniq -c`.

---

## Adding or changing a detector
**A detector that was never seen to fire proves nothing**, the same rule
CLAUDE.md sets for regression tests. When one is added or changed, plant one
defect per thing it claims to catch, each on its own line, in a copy of the
repository, and confirm each one is reported. A detector that also reads
something outside the repository, such as `gh api`, gets its defect planted on
the repository side. A check that runs by script also reports how many files
and items it read, so a silent skip shows.

## Tech Stack
- [ ] Is there any XML layout or ViewBinding usage in a UI screen (instead of Compose)?
      Verified by: human
- [ ] Is there any HTTP client other than Retrofit/OkHttp, or a serialization
      library other than Kotlinx.serialization (e.g. Gson, Moshi)?
      Verified by: human
- [ ] Is there any raw string navigation route (instead of Navigation Compose Safe-type)?
      Verified by: human
- [ ] Has a Light ColorScheme, a `dynamicColor` parameter, or a
      `dynamicDarkColorScheme()` call appeared anywhere? MVP is dark-only with a
      fixed palette — all three are drift, not progress.
      Verified by: human
- [ ] For any library added recently — was a remembered/guessed version used
      instead of checking the latest stable one?
      Verified by: human

## Testing Stack
- [ ] Does any ViewModel test call `Dispatchers.setMain()`/`resetMain()` manually
      instead of using `MainDispatcherRule`?
      Verified by: human
- [ ] Is any mock created with something other than MockK?
      Verified by: human
- [ ] Is any Flow/Channel asserted without Turbine's `.test { }` block?
      Verified by: human
- [ ] Has any test under `test/` started requiring an emulator, a real database,
      or a real network call? Or Robolectric anywhere other than a Compose screen
      test? (JVM-only is the gate — see CLAUDE.md.)
      Verified by: human
- [ ] Does `app/src/test/resources/robolectric.properties`'s `sdk=` disagree with
      `targetSdk`? Nothing enforces the match, and the failure is silent: screen
      tests keep passing on the older API while the app ships the newer one.
      Verified by: human
- [ ] Does any document record an exact measured figure (timings, counts,
      percentages) that nothing regenerates? Three went stale within one branch.
      State the shape and the command to re-derive it instead.
      Verified by: subagent (interpretive)
- [ ] Is there a "DAO test" that only asserts against its own mock? Tautological —
      it proves nothing and hides the real coverage gap.
      Verified by: subagent (interpretive)
- [ ] Did any regression test land without being seen to fail against the code it
      guards? A test written after the fix can pass either way — same failure
      mode as the tautological DAO test above, one layer up.
      Verified by: subagent (interpretive)

## Architecture
- [ ] Does any ViewModel or Composable call a Repository function directly,
      skipping the UseCase?
      Verified by: human
- [ ] Does any Repository implementation contain business logic beyond
      "coordinate data sources" — calculating derived values or applying domain
      rules? (Deliberate exception, not drift: the `updatedAt`/`completedAt`
      write rules on `UserGameEntity`. Everything else still counts.)
      Verified by: subagent (interpretive)
- [ ] Does any file under `domain/` import `retrofit2.*`, `androidx.room.*`,
      or any `android.*` package (other than `javax.inject`)?
      Verified by: human
- [ ] Is `LiveData` used anywhere in the codebase?
      Verified by: human
- [ ] Does any Composable contain logic beyond view/rendering logic
      (e.g. business rules, direct data transformation)?
      Verified by: subagent (interpretive)
- [ ] Does the bottom navigation have anything other than exactly 3 destinations,
      or has Detail been promoted into it?
      Verified by: human
- [ ] Is `Detail` registered once at top level instead of inside every tab's
      nested graph, or does any bottom nav item / `navigateToBottomNavDestination()`
      call target a tab's screen route instead of its `*Graph` route? Either one
      breaks per-tab back stacks (Detail leaks across tabs, no tab selected).
      Verified by: human
- [ ] Does any navigation code call `NavController.currentBackStack`? It is
      `@RestrictTo` library-group API and fails the `lint` gate.
      Verified by: ./gradlew lint
- [ ] Does any suspend function build a `Result` with `runCatching`? It swallows
      `CancellationException`, reporting a cancelled coroutine as a real failure.
      Verified by: human
- [ ] Are any `UiState`/`UiEvent`/`UiEffect` types declared outside their screen's
      `{ScreenName}Contract.kt`?
      Verified by: human
- [ ] Is any one-shot event (navigation, snackbar) modeled in `UiState` instead of
      `UiEffect`? It will re-fire on every recomposition.
      Verified by: subagent (interpretive)
- [ ] Does any ViewModel expose a public entry point other than `handleEvent()`?
      Verified by: human

## Data Sources
- [ ] Does any IGDB endpoint use @GET or @Query instead of @POST with a plain-text Apicalypse body?
      Verified by: human
- [ ] Does any IGDB endpoint declare `Client-ID`/`Authorization` as `@Header`
      parameters, or does any UseCase/ViewModel fetch a token manually?
      `AuthInterceptor` is the only place either may happen.
      Verified by: human
- [ ] Has a second IGDB Retrofit instance or OkHttp client appeared, bypassing
      the one that carries `AuthInterceptor`?
      Verified by: human
- [ ] Is the personal rating/list data (Room) ever being sent to or merged
      with IGDB's community rating anywhere in the code?
      Verified by: subagent (interpretive)
- [ ] Does any Room enum column lack a `TypeConverter`, or does a converter
      persist the enum's **ordinal** rather than its name?
      Verified by: human
- [ ] Was an Entity's schema changed without bumping the Database `version`?
      Verified by: human
- [ ] Is `fallbackToDestructiveMigration()` still present now that the app has
      shipped to real users? (Fine pre-release; data loss after.)
      Verified by: human

## Code Conventions
- [ ] Is any class not PascalCase, or any function not camelCase?
      Verified by: human
- [ ] Does any DTO or Entity type appear in a signature reachable from the Domain
      or UI layer? (Both `toDomain()` and `toEntity()` are legitimate — the rule
      is about where the *types* surface, not which direction the mapper runs.)
      Verified by: human
- [ ] Does any DTO → Entity mapper exist? (Must convert through the Domain model.)
      Verified by: human
- [ ] Does any mapper read the clock (`System.currentTimeMillis()`) or stored
      state, instead of being a pure field-for-field conversion?
      Verified by: human
- [ ] Is there any code comment or identifier written in Spanish?
      Verified by: human
- [ ] Does any comment restate what the code says, narrate *how we got here*
      (a past bug, an earlier version, which review found it), or duplicate text
      that already exists in a Skill, an ADR or a commit message, or sit away from
      the line it constrains? A file past ~15% comment lines is where to look first.
      Verified by: subagent (interpretive)
- [ ] Does any comment block run past 4 lines in a hand-written file other than
      documentation, generated or template text excepted (ADR 0037)?
      Verified by: human
- [ ] Is there any hardcoded UI string in a ViewModel or UseCase instead of `UiText`?
      Verified by: human
- [ ] Is any Hilt module named for a layer or lifetime rather than a concern
      (`AppModule`, `CommonModule`), or does any `@Provides` exist for a class
      that is already constructor-injectable?
      Verified by: human

## Documentation integrity
These check the harness itself, not the code — the failure mode that produced
this checklist's own past drift. A governance diff runs every item under
*Every run*, and each item under *When the diff touches what it reads* whose
`Runs when:` names a path the diff touches
([ADR 0026](decisions/0026-a-detector-runs-when-its-input-changes.md)).
A `Runs when:` line is one line, however long, holding nothing but
repository-relative paths in backticks, joined by commas or `or`. A path ending
in `/` matches anything under it; any other path matches that one file exactly.
A rename touches both its paths, so list the diff with `git fetch` and then
`git diff --name-only --no-renames origin/main...HEAD`.

### Every run
- [ ] Does any document reference a component, Skill, principle, file or section
      that isn't defined anywhere or marked `(PENDING — not yet defined)`?
      Deleting a section is the usual cause — check what pointed at it. That
      includes every path a `Runs when:` line names and every section an ADR's
      `Enforced by:` line cites, wherever the diff renamed or deleted it.
      Verified by: human
- [ ] Did a recent implementation decision (a fallback, a mapping, a naming rule)
      get made in code without being written down anywhere? A diff that only
      rewrites documents can still unwrite a decision the code took, so this runs
      whether or not code changed.
      Verified by: subagent (interpretive)
- [ ] Does any ADR claim a GitHub mechanism that the repository's real settings no
      longer back, or does a setting police a clause an ADR calls unwatched?
      Settings change on GitHub without any diff, so no path can gate this one.
      Read every line from `grep -H "^Enforced by: \(branch protection\|repository settings\)"
      docs/project/decisions/[0-9]*.md`, take the `` `field=value` `` each one
      names, and compare it with `gh api repos/{owner}/{repo}` for a repository
      setting and `gh api repos/{owner}/{repo}/branches/main/protection` for a
      protection rule. Read `.enabled` on a toggle such as `enforce_admins`; for
      `required_status_checks` compare `.contexts`; for a bare field such as
      `required_pull_request_reviews` the object being present is the claim, and
      neither of those two has an `.enabled` to read. A line naming neither field
      nor value is the same finding, and so is a `branch protection` line that
      omits `enforce_admins=false` while the response says it. Then run it the
      other way: for each rule the protection response turns on, and each merge
      setting the repository response turns off (`allow_*_merge`), does an ADR
      clause it polices still sit under `none`? That is a mechanism the map does
      not know about. Skip an ADR superseded in whole, whose `Enforced by:` is
      left as it was.
      Verified by: human

### When the diff touches what it reads
- [ ] Does CLAUDE.md's "Available Skills" list differ from the actual contents
      of `.claude/skills/`, or from the Spec's copy of that list? (Retired
      Skills are listed separately in CLAUDE.md and archived under
      `docs/project/retired-skills/` — one of those appearing as invocable is
      the same drift.)
      Runs when: `.claude/skills/`, `CLAUDE.md`, `docs/project/GameStack-Spec-v1.md` or `docs/project/retired-skills/`
      Verified by: human
- [ ] Does DESIGN.md's prose reference a token its own token block doesn't define,
      or state a value that contradicts it (colors, radii, type roles)?
      Runs when: `docs/project/DESIGN.md`
      Verified by: human
- [ ] Does any ADR under `docs/project/decisions/` break the field rules its own
      README sets — a missing `Backfilled:`, `Status:` or `Enforced by:` line, an
      `Enforced by:` value outside the closed vocabulary, several `Enforced by:`
      lines of which any names no scope, a scoped `Status:` line with no unscoped
      `Accepted` line beside it, a `Date:` that disagrees with the row
      `Backfilled:` cites, quoted opening words that no `grep -F` against
      `DRIFT-LOG.md` resolves, or a cited commit not on `main`
      (`git merge-base --is-ancestor <sha> origin/main`) or PR that does not
      exist (`gh pr view <n>`)? Or does the diff change the frozen `DRIFT-LOG.md`
      at all, the change that froze it excepted (ADR 0033)? Four addressing
      rules, each earned by a false result: anchor `Enforced by:` at line start
      and read every one of them, because ADR prose quotes the field name too
      and one ADR now carries several;
      match each value as a literal prefix of the vocabulary, because some values
      contain ` — ` themselves; resolve *every* row, commit or PR a
      `Backfilled:` line cites, not only the first; and take the cited date as the one immediately before
      the quoted words, which may carry a date of their own.
      Runs when: `docs/project/decisions/` or `docs/project/DRIFT-LOG.md`
      Verified by: human
- [ ] Does an ADR's `claude settings` line describe a rule that
      `.claude/settings.json` no longer carries, or does a `deny` or `ask` rule
      there police a clause an ADR still lists under `none`? Read every line from
      `grep -H "^Enforced by: claude settings" docs/project/decisions/[0-9]*.md`
      and find the patterns it describes under `permissions.deny` or
      `permissions.ask`, in the list the line names. Only the committed file
      counts.
      Runs when: `.claude/settings.json` or `docs/project/decisions/`
      Verified by: human
- [ ] Does any item in this file lack a `Verified by:` line, carry more than one,
      or carry a value outside the closed vocabulary the header declares? An
      unclassified item is invisible to the checker in `TOOLING-BACKLOG.md`
      item 3, whose scope this file is. Under *Documentation integrity*, does an
      item under *When the diff touches what it reads* lack exactly one
      `Runs when:` line (anchored at line start, since prose quotes the field), does one under *Every run* carry any, or does a
      `Runs when:` hold anything but paths as the section header defines them,
      or name a path that does not exist? Any of those never runs, or runs on a
      rule nobody wrote.
      Runs when: `docs/project/DRIFT-CHECKLIST.md`
      Verified by: human
