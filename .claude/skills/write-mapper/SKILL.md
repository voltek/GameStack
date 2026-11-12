---
name: write-mapper
description: Create or change a Mapper between a DTO or Entity and a domain model — `toDomain()` on the read path, `toEntity()` on the write path. Use when asked for a Mapper, or when a DTO, an Entity or a domain model gains, loses or renames a field.
---

## What a Mapper is
A function whose signature crosses the Data/Domain boundary:

| Direction | Function | Example |
|---|---|---|
| Read | an extension on the DTO or Entity, `.toDomain()` | `GameDto.toDomain()` |
| Write | an extension on the Domain model, `.toEntity()` | `UserGame.toEntity(...)` |

Not a Mapper, so not here:
- A conversion inside one layer, such as building an updated Entity to save: a
  private helper in the Repository.
- DTO → Entity: never written. Go through the Domain model, so a change to the API
  cannot reshape the database (`.claude/rules/data-layer.md`).
- Domain → DTO: IGDB is read-only (PRODUCT.md, Constraints on every capability), so
  a requirement that needs one is wrong: stop and ask.

## Writing it
- File `{DataType}Mapper.kt`, named for the DTO or Entity in both directions, in
  `core/data/mapper/`, or that feature's `data/mapper/` only for a type that, per the
  PRD, no other feature reads, as for its Repository (`write-repository`).
  An Entity's `toDomain()` and `toEntity()` share one file, so the two stay in step.
- The reference is `core/data/mapper/GameDtoMapper.kt`.
- Assign every field of the target by name, including those with a default value: a
  field left to its default compiles and silently loses the data.
- Absence keeps its meaning. A missing value stays `null` in the Domain model
  (`coverUrl`, `developer`); a missing list becomes `emptyList()`, which says the same
  thing. Never a sentinel such as `0.0` for "no rating", which the UI would show as a
  rating, and never `!!`.
- A nested object gets its own `.toDomain()` when it maps to a Domain model of its
  own; when it flattens into a field, as a genre into its name, map it inline.
- A value IGDB encodes in a format, such as an image URL, is decoded here, since only
  the data layer knows the format.

## The write path
Not exercised yet (#106): no Entity exists.
The first branch to run it corrects it and removes this note.

`toEntity()` copies fields and computes nothing that depends on the clock or on the
stored row: `updatedAt` and `completedAt` are the Repository's (ADR 0002). When the
Domain model does not carry them, take them as parameters —
`fun UserGame.toEntity(updatedAt: Long, completedAt: Long?)` — so the mapper stays
deterministic and the Repository keeps the decision. Never read the clock in a mapper.

## Tests
Invoke `write-tests`. A mapper test covers every field, each nullable field when
absent, nested lists and an empty or missing list, and for a write path the round trip
`toEntity().toDomain()`.
