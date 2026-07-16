# new-repository-impl Skill

Source: `.claude/skills/new-repository-impl/SKILL.md`

## Purpose
Implements a Repository interface — coordinates DAO/API calls, invokes `new-mapper` for DTO/Entity → Domain conversions, and handles cache-first patterns (emit cached data, then fetch and update).

## Key design decisions
- Room cannot persist enums natively — requires a `TypeConverter` (String ↔ enum), handling null safely.
- Data-layer-only transformations (e.g. building an updated Entity copy) are NOT Mappers — they're private helper functions inside the RepositoryImpl.
- When merging multiple sources into one domain model (e.g. remote game info + local rating), the merge happens inside the Repository — never exposed as separate sources to UseCase/ViewModel.
- Read and write paths for the same data stay in the same Repository, not split across two.

## Iteration history
- v1: initial version