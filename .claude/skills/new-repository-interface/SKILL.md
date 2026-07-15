---
name: new-repository-interface
description: Create a Repository interface in the Domain layer. Use when asked to create a Repository interface, or as part of the new-feature skill.
---

## Construction
- Defines the contract only — no implementation logic.
- Each function is either `suspend fun` (single result) or returns `Flow<T>` (observable stream).
- All parameters and return types use Domain models only — never DTOs, Entities, or Android types.
- Wrap results that can fail in `Result<T>` (e.g. `Flow<Result<List<Game>>>`).
- Include both read and write functions when the feature needs them — do not build
  a write-only or read-only interface if the other direction will be needed soon after.

## Location
`core/domain/repository/` if shared, `feature/{name}/domain/repository/` if feature-specific.

## Naming convention
`{Name}Repository`
Example: `GamesRepository`, `UserGameRepository`

## Quality criteria
- No implementation details leak into the interface (no DAO, no API, no Retrofit/Room types).
- Function names describe intent, not mechanism — e.g. `getPopularGames()`, not `fetchFromApiOrCache()`.
- Before finalizing, double-check both read and write paths are covered if the
  feature will need to display data back to the user (not just persist it).
