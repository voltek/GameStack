---
name: new-usecase
description: Create a new UseCase that invokes a Repository function. Use when asked to create a UseCase for GameStack's Domain layer.
---

## Construction
- Must receive a Repository instance (e.g. `GameRepository`) via constructor, injected using Hilt.
- Always uses `operator fun invoke()`, where the main logic is placed.
- Must return `Flow<Result<T>>` for observable data, or be a `suspend operator fun invoke()`
  for one-shot write operations (e.g. saving a rating).
- Utility/private functions can live inside the class to keep `invoke()` clean,
  for operations that support the main logic without cluttering it.

## Location
- Feature-specific UseCase (used only within one feature):
  `feature/{feature-name}/domain/usecase/`
  Example: `feature/search/domain/usecase/GetPopularGamesUseCase.kt`

- Shared UseCase (used across multiple features):
  `core/domain/usecase/`
  Example: a UseCase that fetches or updates the user's rating for a game,
  used both in the detail screen and in the library screen.

When in doubt, start feature-specific. Move to `core/domain/usecase/`
only once a second feature actually needs it.

## Naming convention
Follows the name of the Repository function it invokes, in PascalCase, with the `UseCase` suffix.
Example: repository function `getPopularGames()` → `GetPopularGamesUseCase`

## Quality criteria
- Errors from the Repository must be propagated, not swallowed or transformed silently.
- Must use only Domain models — never DTOs, Entities, or Android framework types (except Hilt's `javax.inject`).
- No business logic that belongs in the Repository — the UseCase orchestrates, it does not coordinate data sources itself.
- After creating the UseCase, invoke the `write-tests` skill to generate its corresponding unit tests.
