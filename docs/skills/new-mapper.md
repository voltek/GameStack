# new-mapper Skill

Source: `.claude/skills/new-mapper/SKILL.md`

## Purpose
Converts a DTO/Entity to a Domain model via a `.toDomain()` extension function. Does not cover Entity → Entity conversions — those are Repository-internal persistence logic, not mapping.

## Key design decisions
- Every field mapped explicitly — no spread operators or reflection, no `!!` on nullable fields.
- Nested DTOs/Entities get their own separate `.toDomain()` extension before mapping the parent.
- Location: `core/data/mapper/` by default, feature-specific only if clearly isolated to one feature.

## Iteration history
- v1: drafted by Alan, refined collaboratively