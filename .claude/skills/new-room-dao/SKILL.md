---
name: new-room-dao
description: Create a Room Entity, its DAO, any required TypeConverters, and register both on the app Database. Use when a feature needs local persistence, or changes an Entity, its columns or a stored enum, or as part of the new-repository-impl skill.
---

Not exercised yet (#106): no Room code exists.
The first branch to run it corrects it and removes this note.

## Scope
Everything Room-specific below the Repository: the `@Entity`, the `@Dao`, any
`@TypeConverter`, the `@Database` class and its schema. Not the Repository
(`new-repository-impl`), the Domain↔Entity conversion (`new-mapper`) or the DI
bindings (`new-hilt-module`).

## Entity
- `@Entity(tableName = "...")`, snake_case.
- One primary key unless the data needs a composite one. `UserGameEntity`'s is
  `gameId`, the IGDB id, so touching a game twice updates one row.
- Timestamps are `Long` epoch millis, nullable when the event may not have happened.
  A formatted date string cannot be formatted again for another locale.
- "Unset" is a nullable column, never a sentinel such as `""` or `-1`.

## DAO
- A one-shot function is `suspend`; a function returning `Flow` is not, and Room
  rejects the combination at compile time.
- Reads that feed the UI return `Flow`, so a screen updates itself after a write. The
  read inside a read-modify-write is a one-shot `suspend` returning a nullable Entity.
- `@Upsert` when the row may or may not exist, the normal case for a user's action.
- What a query that finds nothing gives depends on its return type, not on `Flow`:
  a list is empty, a single Entity is `null`. So a single Entity is declared nullable,
  in a `Flow` too (`Flow<UserGameEntity?>` for a game the user never touched).
- Name functions for intent (`observeLibraryGames`, `getUserGame`), not SQL.

## Enum columns
Room's own converter persists an enum column as its constant's name: write no
`@TypeConverter` for it (ADR 0068). On reading a value no constant matches, Room
throws, and every query reading that row fails. So renaming or removing a constant
changes the stored data, and nothing in the schema shows it: before the first release
clear the app's data, as for a schema change (§ Database); from the first release on,
bump the version and ship a `Migration` that rewrites the stored values. Each stored
enum gets the test that pins its names (`write-tests`, `references/room-tests.md`),
so a rename cannot pass unnoticed. A `@TypeConverter` is for a type Room cannot store
at all, in `core/data/local/converter/{Name}Converters.kt`, registered with
`@TypeConverters` on the Database.

## Database
- One class: `core/data/local/GameStackDatabase.kt`, abstract, extending
  `RoomDatabase`, with an abstract getter per DAO. Every Entity is in its `entities`;
  a DAO reading an unregistered Entity fails the build.
- The build exports its schema to `app/schemas/` through the Room Gradle plugin
  (ADR 0069); commit the JSON with the change that produced it. A project whose
  `gradle/libs.versions.toml` has no `androidx.room` plugin sets it up as ADR 0069
  says first: without it Room writes no schema.
- Until the first release (PRODUCT.md § First release; #114 records it and checks the
  committed schema then) the version stays 1. After a
  schema change, run `adb shell pm clear com.gamestack` on the development device
  before launching: Room refuses to open a database whose schema no longer matches
  ("Room cannot verify the data integrity"). A reinstall alone keeps the data.
- From the first release on, every schema change bumps `version` and ships a
  `Migration`: losing a user's library is not an acceptable upgrade. Never
  `fallbackToDestructiveMigration()`.

## Location
`core/data/local/entity/{Name}Entity.kt`, `core/data/local/dao/{Name}Dao.kt` and the
Database above. Room is shared infrastructure, so it lives in `core/` even while one
feature reads it.

## Steps
1. Create or extend the Entity and the DAO.
2. Register them on the Database, handle a schema change as § Database says, and
   commit the exported schema.
3. Invoke `new-hilt-module` to provide the Database and the DAO.
4. Invoke `new-mapper` for `toDomain()` and `toEntity()`.

## Tests
A DAO is not unit-tested: it needs real SQLite, and the instrumented suite for DAOs
and migrations is backlog (#132). What covers it until then is in `write-tests`
§ Stack and scope. No business logic goes in a DAO: when a column is written is the
Repository's rule (ADR 0002).
