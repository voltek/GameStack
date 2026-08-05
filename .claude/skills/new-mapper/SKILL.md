---
name: new-mapper
description: Create a Mapper that converts a DTO or Entity to a domain model (read path), or a domain model to a Room Entity (write path), following GameStack's mapping conventions. Also invoked as part of the new-feature skill.
---

## The two directions
A Mapper converts **across the Data/Domain boundary**, in either direction:

| Direction | Function | When |
|---|---|---|
| Read path | `GameDto.toDomain()`, `UserGameEntity.toDomain()` | Inbound data becomes a Domain model |
| Write path | `UserGame.toEntity()` | A Domain model is persisted to Room |

The write path exists because every write feature (saving a rating, moving a game
between lists) has to build an Entity from Domain data, and that conversion needs a
defined home. It does not break the Domain/Data boundary: its input is a Domain
model and its output never leaves the Data layer (see CLAUDE.md's Mapper rule).

## When this skill does NOT apply
- **Same-layer conversion** (e.g. Entity → Entity, building an updated copy for
  persistence): NOT a Mapper. These belong as private helper functions inside the
  Repository implementation. Rule of thumb: if the function's signature doesn't
  cross the Data/Domain boundary, it isn't a Mapper.
- **DTO → Entity** (remote model straight to local model): never write this.
  Remote and local models must not know about each other — convert through the
  Domain model (`dto.toDomain().toEntity()`), so a change to the API shape can't
  silently reshape the database.
- **Domain → DTO**: not applicable in this project. IGDB is read-only (CLAUDE.md,
  Tier 1) — nothing is ever serialized back to it. If you think you need one, the
  requirement is wrong; stop and ask.

## Location
- Shared model (used by more than one feature) → `core/data/mapper/`
- Feature-specific model (used only within one feature) → `feature/{name}/data/mapper/`

When in doubt, prefer `core/data/mapper/`. Move to feature-specific only
if the model is clearly isolated to a single feature.

## Steps

### 1. Create the Mapper file
- Follow the location rule above
- Name the file after the **Data-layer** type in the conversion, whichever
  direction it goes: `{DtoOrEntityClassName}Mapper.kt`
  Example: `GameDtoMapper.kt` for `GameDto`; `UserGameEntityMapper.kt` holds
  **both** `UserGameEntity.toDomain()` and `UserGame.toEntity()` — the two
  directions for one table belong in one file, since they must stay in sync.

### 2. Write the mapping extension function
- Read path: an extension function on the DTO/Entity named `.toDomain()`,
  returning the corresponding domain model.
- Write path: an extension function on the **Domain model** named `.toEntity()`,
  returning the Room Entity.
- Map every field explicitly — do not use spread operators or reflection

```kotlin
// Example pattern
fun GameDto.toDomain() = Game(
    id = this.id,
    name = this.name,
    coverUrl = this.cover?.url,      // handle nullable fields explicitly
    rating = this.totalRating ?: 0.0 // provide safe defaults for nulls
)
```

### 3. Handle nested lists
If any field is a list of nested DTOs/Entities, create a separate extension function
for the nested type before mapping the parent.

```kotlin
fun GenreDto.toDomain() = Genre(id = this.id, name = this.name)

fun GameDto.toDomain() = Game(
    genres = this.genres?.map { it.toDomain() } ?: emptyList()
)
```

### 4. Write path only — do not invent Repository-owned fields
A `toEntity()` mapper is a **pure field-for-field conversion**. It must not
compute values that depend on stored state or on the clock:
`updatedAt` and `completedAt` on `UserGameEntity` are stamped by the Repository
during its read-modify-write cycle (CLAUDE.md, Data Sources → Tier 2), because
deciding them requires comparing against the row already in the database.

If the Domain model does not carry those values, take them as parameters —
`fun UserGame.toEntity(updatedAt: Long, completedAt: Long?)` — so the Repository
stays the one deciding them and the mapper stays deterministic and testable.
Never call `System.currentTimeMillis()` inside a mapper: it makes the function
untestable and silently moves a persistence rule out of its documented home.

### 5. Write the Unit Test
Create a test file at the mirror path under `test/`:
- `GameDtoMapperTest.kt`

Test must cover:
- Happy path: all fields populated correctly
- Null fields: nullable DTO/Entity fields produce safe defaults in domain model
- Nested lists: nested DTOs map correctly to domain objects
- Empty list: empty or null list from DTO/Entity produces emptyList() in domain model
- Write path (`toEntity()`): every Domain field lands on the correct Entity
  column, and enum fields survive the round trip (`toEntity().toDomain()`
  returns the original Domain model) — this is what catches a TypeConverter
  or column mismatch before it reaches the database.

## Naming conventions
- File: `{DtoOrEntityClassName}Mapper.kt` — named after the Data-layer type in
  both directions
- Read-path extension function: `.toDomain()`
- Write-path extension function: `.toEntity()`
- Nested helper functions: `.toDomain()` on each nested DTO/Entity type

## Quality criteria
- A DTO/Entity type never appears in a signature reachable from the Domain or UI
  layer — this, not "mappers only return Domain models", is the boundary rule
- Nullable DTO/Entity fields must always have a safe fallback — no `!!` operators
- All fields of the target model must be explicitly assigned — no missing fields
- The mapper file contains no business logic and no clock/state access —
  only type conversion
- No DTO → Entity mapper exists anywhere (convert through the Domain model)
- The test covers happy path + all nullable fields + nested lists, plus the
  enum round trip if a write-path mapper was added
