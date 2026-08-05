# DRIFT-CHECKLIST — GameStack

> Manual audit checklist derived from CLAUDE.md's Tier 1 (Immutable) rules.
> Run this periodically, or after a batch of agent-generated features, to detect
> drift between the code and the Project Constitution.
>
> Each item is phrased as a verifiable question, and **every item is written so
> that "no" is the healthy answer** — a "yes" means drift was found. Read them as
> violation detectors, not as goals.
>
> When drift is found, resolve it per the Self-Healing Loop (CLAUDE.md): either
> fix the code, OR update the document if the code turned out to be right and the
> document is what's outdated. Log which direction was chosen below.
>
> Future: this checklist becomes the logic for an automated checker (Hook /
> validator subagent) in Block 4 — Loop Engineering (defined in CLAUDE.md →
> Pending/Roadmap).

---

## Tech Stack
- [ ] Is there any XML layout or ViewBinding usage in a UI screen (instead of Compose)?
- [ ] Is there any HTTP client other than Retrofit/OkHttp, or a serialization
      library other than Kotlinx.serialization (e.g. Gson, Moshi)?
- [ ] Is there any raw string navigation route (instead of Navigation Compose Safe-type)?
- [ ] Has a Light ColorScheme, a `dynamicColor` parameter, or a
      `dynamicDarkColorScheme()` call appeared anywhere? MVP is dark-only with a
      fixed palette — all three are drift, not progress.
- [ ] For any library added recently — was a remembered/guessed version used
      instead of checking the latest stable one?

## Testing Stack
- [ ] Does any ViewModel test call `Dispatchers.setMain()`/`resetMain()` manually
      instead of using `MainDispatcherRule`?
- [ ] Is any mock created with something other than MockK?
- [ ] Is any Flow/Channel asserted without Turbine's `.test { }` block?
- [ ] Has any test under `test/` started requiring Robolectric, an emulator, a real
      database, or a real network call? (JVM-only is the gate — see CLAUDE.md.)
- [ ] Is there a "DAO test" that only asserts against its own mock? Tautological —
      it proves nothing and hides the real coverage gap.

## Architecture
- [ ] Does any ViewModel or Composable call a Repository function directly,
      skipping the UseCase?
- [ ] Does any Repository implementation contain business logic beyond
      "coordinate data sources" — calculating derived values or applying domain
      rules? (Deliberate exception, not drift: the `updatedAt`/`completedAt`
      write rules on `UserGameEntity`. Everything else still counts.)
- [ ] Does any file under `domain/` import `retrofit2.*`, `androidx.room.*`,
      or any `android.*` package (other than `javax.inject`)?
- [ ] Is `LiveData` used anywhere in the codebase?
- [ ] Does any Composable contain logic beyond view/rendering logic
      (e.g. business rules, direct data transformation)?
- [ ] Does the bottom navigation have anything other than exactly 3 destinations,
      or has Detail been promoted into it?
- [ ] Are any `UiState`/`UiEvent`/`UiEffect` types declared outside their screen's
      `{ScreenName}Contract.kt`?
- [ ] Is any one-shot event (navigation, snackbar) modeled in `UiState` instead of
      `UiEffect`? It will re-fire on every recomposition.
- [ ] Does any ViewModel expose a public entry point other than `handleEvent()`?

## Data Sources
- [ ] Does the IGDB API service use `@POST` with a plain-text Apicalypse body
      everywhere — or has a `@GET`/`@Query` endpoint been introduced by mistake?
- [ ] Does any IGDB endpoint declare `Client-ID`/`Authorization` as `@Header`
      parameters, or does any UseCase/ViewModel fetch a token manually?
      `AuthInterceptor` is the only place either may happen.
- [ ] Has a second IGDB Retrofit instance or OkHttp client appeared, bypassing
      the one that carries `AuthInterceptor`?
- [ ] Is the personal rating/list data (Room) ever being sent to or merged
      with IGDB's community rating anywhere in the code?
- [ ] Does any Room enum column lack a `TypeConverter`, or does a converter
      persist the enum's **ordinal** rather than its name?
- [ ] Was an Entity's schema changed without bumping the Database `version`?
- [ ] Is `fallbackToDestructiveMigration()` still present now that the app has
      shipped to real users? (Fine pre-release; data loss after.)

## Code Conventions
- [ ] Is any class not PascalCase, or any function not camelCase?
- [ ] Does any DTO or Entity type appear in a signature reachable from the Domain
      or UI layer? (Both `toDomain()` and `toEntity()` are legitimate — the rule
      is about where the *types* surface, not which direction the mapper runs.)
- [ ] Does any DTO → Entity mapper exist? (Must convert through the Domain model.)
- [ ] Does any mapper read the clock (`System.currentTimeMillis()`) or stored
      state, instead of being a pure field-for-field conversion?
- [ ] Is there any code comment or identifier written in Spanish?
- [ ] Is there any hardcoded UI string in a ViewModel or UseCase instead of `UiText`?
- [ ] Is any Hilt module named for a layer or lifetime rather than a concern
      (`AppModule`, `CommonModule`), or does any `@Provides` exist for a class
      that is already constructor-injectable?

## Documentation integrity
These check the harness itself, not the code — the failure mode that produced
this checklist's own past drift.
- [ ] Does any document reference a component, Skill, principle, or "Block N"
      that isn't defined anywhere or marked `(PENDING — not yet defined)`?
- [ ] Does CLAUDE.md's "Available Skills" list match the actual contents of
      `.claude/skills/`, and the Spec's copy of that list?
- [ ] Does DESIGN.md's prose reference a token its own token block doesn't define,
      or state a value that contradicts it (colors, radii, type roles)?
- [ ] Did a recent implementation decision (a fallback, a mapping, a naming rule)
      get made in code without being written down anywhere?

---

## Resolution log
Use this space to note when a drift was found and how it was resolved —
useful history for understanding recurring patterns.

| Date | Item | Resolution (code fixed / doc updated) |
|---|---|---|
| 2026-08-04 | Checklist demanded Light theme support, contradicting Tier 1 dark-only | Doc updated — item inverted into a violation detector for Light/dynamic color |
| 2026-08-04 | Domain → Entity conversion had no defined home (write path) | Doc updated — `toEntity()` legitimized in CLAUDE.md + `new-mapper`; boundary rule restated as "DTO/Entity types never surface outside Data" |
| 2026-08-04 | `completedAt` transition rule fit in no layer without violating a rule | Doc updated — declared a persistence invariant owned by the Repository, as an explicit documented exception |
| 2026-08-04 | DESIGN.md asked for dynamic color; code and brand identity forbid it | Doc updated — code was right (`Theme.kt` never had it); DESIGN.md, Spec and CLAUDE.md now state the ban |
| 2026-08-04 | DESIGN.md prose specified white button labels; `on-primary` is #3c0091 | Doc updated — prose corrected to use the `on-*` role |
| 2026-08-04 | Skills referenced undefined concepts (Human-in-the-Loop, MVI contracts, Block 4, Self-Healing Loop, `discovery` skill) | Doc updated — all four defined in CLAUDE.md/Spec; the fifth reference removed |
| 2026-08-04 | Room/DAO, Hilt modules and interceptors had no owning Skill | Skills added — `new-room-dao`, `new-hilt-module` |
| 2026-08-04 | Type/Shape/unit mappings decided in code but recorded nowhere | Doc updated — DESIGN.md now carries both mapping tables and the rem→dp rule |
