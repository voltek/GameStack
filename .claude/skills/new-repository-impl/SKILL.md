---
name: new-repository-impl
description: Create or change a Repository — its interface in the Domain layer, its implementation in the Data layer, and its writes to the user's library. Use when asked to create a Repository or a RepositoryImpl, or to add a read or a write to one.
---

## The interface
- In `core/domain/repository/`. A feature's own `domain/repository/` only for data
  that, per the PRD, no other feature reads: the app's data is shared (the user's
  library feeds Detail, Library and Home), and a Repository placed in the first
  feature to need it would move when the second arrives. Named `{Name}Repository`:
  `GamesRepository`, `UserGameRepository` (PENDING — not yet defined; Detail, #106).
- A one-shot call is `suspend fun` returning `Result<T>`; data the UI keeps observing
  returns `Flow<Result<T>>`, so a failure reaches the UseCase as a value it passes on
  (`new-usecase`). Only Domain types in its signatures.
- Name each function for what it gives the caller (`searchGames`), not how
  (`fetchFromApiOrCache`).
- The read and the write of the same data, such as a rating, belong to one
  Repository, so the rules about how that data changes have one home.

## The implementation
- `{Name}RepositoryImpl @Inject constructor(...)`, beside its interface's choice:
  `core/data/repository/`, or that feature's `data/repository/`. It takes its API
  services and DAOs as `private val`, and the Database when it writes (§ Writes).
- Bind it to its interface (`new-hilt-module`): in `RepositoryModule`, or for a
  feature's own Repository in a module under that feature's `data/di/`. Without the
  `@Binds`, Hilt fails the build.
- A new IGDB endpoint and its DTOs: `new-api-service`. Local storage (Entity, DAO,
  converters, Database): `new-room-dao`. Every conversion across the Data/Domain
  boundary: `new-mapper`.
- A suspend function returning `Result` uses `try`/`catch` with `CancellationException`
  rethrown, never `runCatching` (`.claude/rules/code.md`); `GamesRepositoryImpl` is
  the reference. An observed `Flow` maps each value to `Result.success` and ends with
  `.catch { emit(Result.failure(it)) }`, so a failed read, such as Room meeting a
  stored value it cannot read, reaches the screen as a value instead of crashing it.
- A Repository combines the sources of its own data only (ADR 0054). A model that
  joins two kinds of data, such as a game from IGDB with the user's rating from Room,
  is joined by a UseCase from both Repositories (`new-usecase`), so each kind keeps
  its read and its write in one Repository.
- An IGDB response is not cached: offline mode is a Repository redesign of its own
  (#130).

## Writes to `UserGameEntity`
Not exercised yet (#106): no Room code exists.
The first branch to run it corrects it and removes this note.

A write is a read-modify-write, never a blind upsert, because the timestamps depend on
the stored row (`.claude/rules/data-layer.md`, ADR 0054):
1. Read the row for that `gameId`; it may not exist yet.
2. Build the new row: the change, `updatedAt` stamped on every write, and
   `completedAt` stamped only when `listStatus` becomes `COMPLETED` from something
   else.
3. Upsert it.

Rating and list status share the row, so one private helper runs this cycle for
every write and the two cannot drift apart. The helper runs the three steps inside
`database.withTransaction { }`, so the Repository also takes the Database: two quick
writes, a rating and then a list status, would otherwise both read the old row, and
the second upsert would drop the first change. The Repository decides when a column is
written, never what it means to the user: "Completed on [date]" and the order of a
list are Domain or UI work. A rule that needs more than the old and new row is a
product rule: stop and ask.

## Tests
Invoke `write-tests` once the implementation and its mappers exist.
