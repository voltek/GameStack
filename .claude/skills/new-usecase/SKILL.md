---
name: new-usecase
description: Create or change a UseCase in the Domain layer, which calls one or more Repository functions and holds the product rules between them. Use when asked to create a UseCase, or when a ViewModel needs data or an action from a Repository.
---

## Construction
- `class {Name}UseCase @Inject constructor(private val repository: {Name}Repository)`,
  with its logic in `operator fun invoke(...)`.
- A one-shot call, read or write, is `suspend operator fun invoke(...)` returning
  `Result<T>`, as `SearchGamesUseCase` does. Data the screen keeps observing is a plain
  `operator fun invoke(...)` returning `Flow<Result<T>>`.
- Pass the Repository's failure on unchanged. A UseCase that turns a failure into
  success, or into another error, hides from the screen what went wrong.

## What belongs here
Product rules: deriving a value, normalising input before it reaches a Repository
(`SearchGamesUseCase` trims the query and never sends a blank one), combining the
results of several Repository calls, such as a game from `GamesRepository` with the
user's rating from `UserGameRepository` (PENDING — not yet defined; Detail, #106).
A rule about how a write changes stored data, such as stamping `completedAt` when a
game becomes Completed, needs the stored row to compare against, and Entities never
leave the data layer (ADR 0003), so it is the Repository's (ADR 0054).

A UseCase that only forwards to its Repository is still written: the ViewModel takes
a UseCase, never a Repository (`.claude/rules/code.md`).

## Location and name
`feature/{feature}/domain/usecase/` while one feature uses it; it moves to
`core/domain/usecase/` when a second feature needs it, not before. A UseCase calling
one Repository function takes its name, in PascalCase with `UseCase`: `searchGames()`
→ `SearchGamesUseCase`. One joining several is named for what it gives the screen:
`GetGameDetailUseCase`.

## Tests
Invoke `write-tests` for its unit tests.
