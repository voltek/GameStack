---
name: new-room-dao
description: Create a Room Entity, its DAO, any required TypeConverters, and register both on the app Database. Use when a feature needs local persistence, or as part of the new-repository-impl skill.
---

## Scope
This skill owns everything Room-specific below the Repository: the `@Entity`,
the `@Dao`, `@TypeConverter`s, and the `@Database` class that ties them together.
The Repository consumes the DAO but never defines it — that split is why this
skill exists (previously nothing owned this step, and it got improvised).

Not in scope: the Repository itself (`new-repository-impl`), the Domain↔Entity
conversion (`new-mapper`), or the DI bindings (`new-hilt-module`).

## Entity

- Annotate `@Entity(tableName = "...")`, snake_case table name.
- Exactly one primary key unless there's a real composite need. For GameStack's
  `UserGameEntity` that's `gameId` — the IGDB id, not a generated one, so a game
  the user interacts with twice updates one row instead of creating a second.
- Entities are Data-layer types. They never appear in a Domain or UI signature
  (CLAUDE.md, Code Conventions → Tier 1).
- Store timestamps as `Long` (epoch millis), nullable when the event may not have
  happened. Do not store formatted date strings — formatting is a UI concern and
  a stored string can't be re-formatted for another locale.
- Model "unset" as a nullable column, never as a sentinel value like `""` or `-1`.

## DAO

- Interface annotated `@Dao`; every function is `suspend fun`, except those
  returning `Flow<T>` — Flow queries are already asynchronous and must NOT be
  `suspend` (Room rejects that combination at compile time).
- Reads that feed the UI return `Flow<T>` so the screen updates itself after a
  write. Reads used internally for a read-modify-write cycle are one-shot
  `suspend fun` returning a nullable Entity.
- Prefer `@Upsert` over separate `@Insert`/`@Update` when the row may or may not
  exist — which is the normal case for user interactions.
- A `Flow` query that finds nothing emits an empty list; a one-shot query that
  finds nothing returns `null`. Handle both — do not assume a row exists.
- Function names describe intent (`observeLibraryGames`, `getUserGame`), not SQL.

## TypeConverters

- Room cannot persist Kotlin enums natively — each needs a `@TypeConverter`
  pair, converting to/from `String` (never the ordinal: reordering the enum
  would silently corrupt every stored row).
- Handle null on both sides: `GameRating?` and `ListStatus?` are nullable by
  design ("unrated", "not in a list").
- On the way back, an unrecognized stored string must not crash — decide and
  document the fallback (return `null`, treating the value as unset).
- Location: `core/data/local/converter/`, registered via `@TypeConverters` on
  the `@Database` class.

## Database

- One `@Database` class for the app: `core/data/local/GameStackDatabase.kt`,
  abstract, extending `RoomDatabase`, exposing an abstract getter per DAO.
- Every new Entity must be added to the `entities` array — a DAO referencing an
  unregistered Entity fails at compile time.
- **Any change to an Entity's schema requires bumping `version`.** Until the app
  ships, `fallbackToDestructiveMigration()` is acceptable (there is no user data
  to preserve). Once it ships, that call must be removed and replaced with a real
  `Migration` — losing a user's library is not an acceptable upgrade path. Flag
  this explicitly whenever the version is bumped.

## Location
- `core/data/local/entity/{Name}Entity.kt`
- `core/data/local/dao/{Name}Dao.kt`
- `core/data/local/converter/{Name}Converters.kt`
- `core/data/local/GameStackDatabase.kt`

Room is shared infrastructure — keep it in `core/`, not under `feature/`, even
when only one feature currently reads it. The single `UserGameEntity` is already
consumed by Home, Library and Detail.

## Naming convention
`{Name}Entity`, `{Name}Dao`, `{Name}Converters`

## Steps
1. Create or extend the Entity.
2. Create or extend the DAO.
3. Add TypeConverters for any enum or non-primitive column.
4. Register the Entity and DAO on the Database, bumping `version` if the schema
   changed.
5. Invoke `new-hilt-module` to provide the Database and DAO instances.
6. Invoke `new-mapper` for the `toDomain()` / `toEntity()` conversions.

## Quality criteria
- No enum column without a matching TypeConverter.
- No `suspend` on a `Flow`-returning DAO function.
- The Entity appears in the Database's `entities` array.
- No business logic in the DAO — queries only. Rules about *when* to write
  (e.g. stamping `completedAt`) belong to the Repository, per CLAUDE.md.
- Testing: DAOs are not unit-tested in this project (they need real SQLite —
  see `write-tests` → Scope). Coverage comes from Repository tests with a mocked
  DAO plus the mapper round-trip test. Do not write a mocked DAO test that only
  asserts the mock — it proves nothing.
