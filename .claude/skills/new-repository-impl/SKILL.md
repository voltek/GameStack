---
name: new-repository-impl
description: Create a RepositoryImpl that implements a Repository interface. Use when asked to create a RepositoryImpl, or as part of the new-feature skill.
---

## Construction
- Receives DAO and/or API service as `private val`, injected via Hilt constructor.
  Register the binding via the `new-hilt-module` skill — a RepositoryImpl with no
  `@Binds` is invisible to the DI graph and fails at compile time.
- Implements the corresponding Repository interface — must override every declared function.
- Create the DTOs (remote) needed to match what the API returns. For local storage,
  invoke the `new-room-dao` skill — it owns the Entity, the DAO, the TypeConverters,
  and the Database registration.
- Invoke the `new-mapper` skill for each conversion across the Data/Domain boundary:
  `.toDomain()` on the read path, `.toEntity()` on the write path.
- Inside every overridden function: call the required DAO/API function, then use the
  corresponding Mapper — `toDomain()` when returning data outward, `toEntity()` when
  persisting inward. Never a direct DTO → Entity conversion (see `new-mapper`).
- If a function needs both local and remote data (cache-first pattern): emit cached data
  first if available, then fetch remote data and update the cache.
- Data-layer-only transformations (e.g. building an updated copy of an Entity to persist,
  merging a partial update into an existing row) are NOT Mappers — implement them as
  private helper functions inside the RepositoryImpl itself.

## Returning `Result` — never `runCatching`
A suspend function returning `Result` must build it with `try`/`catch`, rethrowing
`CancellationException` before catching `Exception`:

```kotlin
override suspend fun searchGames(query: String): Result<List<Game>> =
    try {
        Result.success(api.searchGames(buildQuery(query)).map { it.toDomain() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
```

`runCatching` catches `Throwable`, so it swallows `CancellationException` too and
reports a merely-cancelled coroutine as a genuine failure — the caller then shows
an error for work nobody was waiting on any more (CLAUDE.md, Architecture → Tier 1).

## Write operations — read-modify-write and persistence invariants
Writes to `UserGameEntity` are never blind upserts. The Repository:
1. Loads the existing row for that `gameId` (may be absent — first interaction).
2. Produces the updated copy: the incoming change, plus `updatedAt` stamped on
   every write, plus `completedAt` stamped **only** when `listStatus` is becoming
   COMPLETED and the stored value was not already COMPLETED.
3. Upserts the result.

These timestamp rules are **persistence invariants and belong here** — an explicitly
documented exception to "Repositories contain no business logic" (CLAUDE.md, Data
Sources → Tier 2). They live here because detecting a *transition* requires comparing
against stored state, and Entities never leave the Data layer, so no UseCase can see it.

The boundary that still applies: the Repository decides *when a timestamp column is
written*, never *what it means to the user*. Formatting "Completed on [date]" or
ordering "Recently Interacted" is Domain/UI work. If a rule ever needs more than
comparing old and new persisted state, it is real business logic — stop and ask.

Because rating and list-status writes touch the same row, keep this cycle in one
private helper rather than duplicating it per write function (CLAUDE.md, Tier 3:
avoid duplicating logic that could drift out of sync).

## Local Data Conventions (Room)
- Entities, DAOs, TypeConverters and Database registration are owned by the
  `new-room-dao` skill — invoke it rather than hand-rolling them here. (Reminder
  of the constraint it handles: Kotlin enums need a `TypeConverter`, since Room
  cannot persist them natively.)
- When a function needs to merge data from multiple sources into one domain model
  (e.g. remote game info + local user rating), fetch both, then combine them into the
  domain model inside the Repository — never expose the two sources separately to the
  UseCase/ViewModel layer.

## Location
`core/data/repository/` if shared, `feature/{name}/data/repository/` if feature-specific.

## Naming convention
`{RepositoryName}Impl`
Example: `GamesRepositoryImpl`, `UserGameRepositoryImpl`

## Quality criteria
- Repositories contain no business logic — they only coordinate data sources.
- Every function from the interface is overridden — no partial implementations.
- Never expose DTO/Entity types outside the Repository — only Domain models leave this class.
- Read and write paths for the same data (e.g. rating, list status) live in the same
  Repository — do not split them across two Repositories unless they are genuinely
  unrelated concerns.
- After creating the RepositoryImpl and its Mappers, invoke the `write-tests` skill.
