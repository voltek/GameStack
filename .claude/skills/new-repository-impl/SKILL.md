---
name: new-repository-impl
description: Create a RepositoryImpl that implements a Repository interface. Use when asked to create a RepositoryImpl, or as part of the new-feature skill.
---

## Construction
- Receives DAO and/or API service as `private val`, injected via Hilt constructor.
- Implements the corresponding Repository interface — must override every declared function.
- Create the DTOs (remote) and/or Entities (local) needed to match what the DAO/API returns.
- Invoke the `new-mapper` skill for each DTO/Entity that needs conversion to a Domain model.
- Inside every overridden function: call the required DAO/API function, then use the
  corresponding Mapper to convert Entity/DTO ↔ Domain model.
- If a function needs both local and remote data (cache-first pattern): emit cached data
  first if available, then fetch remote data and update the cache.
- Data-layer-only transformations (e.g. building an updated copy of an Entity to persist,
  merging a partial update into an existing row) are NOT Mappers — implement them as
  private helper functions inside the RepositoryImpl itself.

## Local Data Conventions (Room)
- Kotlin enums stored in Room require a `TypeConverter` — Room cannot persist enums
  natively. Convert to/from String, handling null safely for "unrated" or "unset" states.
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
