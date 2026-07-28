# DRIFT-CHECKLIST — GameStack

> Manual audit checklist derived from CLAUDE.md's Tier 1 (Immutable) rules.
> Run this periodically, or after a batch of agent-generated features, to detect
> drift between the code and the Project Constitution.
>
> Each item is phrased as a verifiable question — the answer should be checkable
> by reading code, not by opinion. If the answer is "no", resolve per the
> Self-Healing Loop: either fix the code, OR update CLAUDE.md if the code turned
> out to be right and the document is what's outdated.
>
> Future: this checklist becomes the logic for an automated checker (Hook /
> validator subagent) in Block 4 — Loop Engineering.

---

## Tech Stack
- [ ] Is every new UI screen built with Jetpack Compose only (no XML layouts, no ViewBinding)?
- [ ] Is all networking code going through Retrofit (no other HTTP client introduced)?
- [ ] Do all navigation routes use Navigation Compose Safe-type (no raw string routes)?
- [ ] Does every screen support both Dark and Light theme (Material3)?
- [ ] For any library added recently — was its latest stable version actually checked,
      or was a remembered/guessed version used?

## Testing Stack
- [ ] Do all ViewModel tests use `MainDispatcherRule` instead of manual
      `Dispatchers.setMain()`/`resetMain()` calls?
- [ ] Are all mocks created with MockK (no other mocking library present)?
- [ ] Are Flow/Channel assertions using Turbine's `.test { }` block?

## Architecture
- [ ] Does any ViewModel or Composable call a Repository function directly,
      skipping the UseCase?
- [ ] Does any Repository implementation contain conditional business logic
      beyond "coordinate data sources" (e.g. calculating derived values,
      applying domain rules)?
- [ ] Does any file under `domain/` import `retrofit2.*`, `androidx.room.*`,
      or any `android.*` package (other than `javax.inject`)?
- [ ] Is `LiveData` used anywhere in the codebase?
- [ ] Does any Composable contain logic beyond view/rendering logic
      (e.g. business rules, direct data transformation)?

## Data Sources
- [ ] Does the IGDB API service use `@POST` with a plain-text Apicalypse body
      everywhere — or has a `@GET`/`@Query` endpoint been introduced by mistake?
- [ ] Is the personal rating/list data (Room) ever being sent to or merged
      with IGDB's community rating anywhere in the code?

## Code Conventions
- [ ] Are all classes PascalCase and all functions camelCase, without exception?
- [ ] Do all Mappers live in the data layer and return Domain models only
      (never expose a DTO/Entity type past the Mapper)?
- [ ] Is there any code comment or identifier written in Spanish?
- [ ] Is there any hardcoded UI string in a ViewModel or UseCase instead of `UiText`?

---

## Resolution log
Use this space to note when a drift was found and how it was resolved —
useful history for understanding recurring patterns.

| Date | Item | Resolution (code fixed / doc updated) |
|---|---|---|
| | | |
