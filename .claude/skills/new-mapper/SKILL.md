---
name: new-mapper
description: Create a Mapper that converts a DTO or Entity to a domain model, following GameStack's mapping conventions. Also invoked as part of the new-feature skill.
---

## When this skill does NOT apply
If the function converts between two objects of the same layer (e.g. Entity → Entity,
building an updated copy for persistence), it is NOT a Mapper. Mappers only convert
toward the Domain model. Data-layer-only transformations belong as private helper
functions inside the Repository implementation instead.

## Location
- Shared model (used by more than one feature) → `core/data/mapper/`
- Feature-specific model (used only within one feature) → `feature/{name}/data/mapper/`

When in doubt, prefer `core/data/mapper/`. Move to feature-specific only
if the model is clearly isolated to a single feature.

## Steps

### 1. Create the Mapper file
- Follow the location rule above
- Name the file: `{DtoOrEntityClassName}Mapper.kt`
  Example: `GameDtoMapper.kt` for `GameDto`

### 2. Write the mapping extension function
- Create an extension function on the DTO/Entity class named `.toDomain()`
- Return the corresponding domain model
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

### 4. Write the Unit Test
Create a test file at the mirror path under `test/`:
- `GameDtoMapperTest.kt`

Test must cover:
- Happy path: all fields populated correctly
- Null fields: nullable DTO/Entity fields produce safe defaults in domain model
- Nested lists: nested DTOs map correctly to domain objects
- Empty list: empty or null list from DTO/Entity produces emptyList() in domain model

## Naming conventions
- File: `{DtoOrEntityClassName}Mapper.kt`
- Extension function: `.toDomain()`
- Nested helper functions: `.toDomain()` on each nested DTO/Entity type

## Quality criteria
- The domain model returned must never expose raw DTO/Entity types
- Nullable DTO/Entity fields must always have a safe fallback — no `!!` operators
- All fields of the domain model must be explicitly assigned — no missing fields
- The mapper file contains no business logic — only type conversion
- The test covers happy path + all nullable fields + nested lists
