# ADR 0068 — An enum column uses Room's own converter

Status: Accepted
Date: 2026-11-11
Backfilled: no
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Data Sources — an enum column with a hand-written converter, and a constant renamed or removed after the first release without a version bump and a `Migration`
Enforced by: none (prose only — .claude/skills/write-tests/SKILL.md § Stored enum) — a test pinning each stored enum's names
Enforced by: none (prose only — .claude/skills/new-room-dao/SKILL.md § Enum columns) — a `@TypeConverter` only for a type Room cannot store

## Context
`new-room-dao`, `new-repository-impl` and `write-tests` said Room could not persist a
Kotlin enum, and asked for a hand-written `@TypeConverter` per enum column, with a
fallback to `null` for a stored value no constant matched. No Room code existed yet;
`UserGameEntity`, with its rating and list status, was to be the first. Room has had a
default enum converter since 2.3.0, and a hand-written one takes precedence over it
(Room's release notes and "Referencing complex data"). The code Room 2.8.4 generated
for a probe Entity stored the constant's name and, for an unknown value, threw
`IllegalArgumentException("Can't convert value to enum, unknown value: …")`. An unknown
value could only come from renaming or removing a constant. Such a rename left the
column TEXT, so the exported schema and its hash did not change, and neither Room,
the build nor a schema-validating migration test noticed it.

## Decision
- An enum column has no hand-written `@TypeConverter`: Room's own stores the
  constant's name.
- Renaming or removing a constant of a stored enum changes the stored data. Before
  the first release it is handled as a schema change (ADR 0069); from the first
  release on, it bumps the Database version and ships a `Migration` that rewrites
  the stored values.
- Each stored enum has a JVM test asserting its constants' names, whose message
  names this rule, so a rename fails the gate wherever the enum lives.
- A `@TypeConverter` is written only for a type Room cannot store.

## Consequences
The converter class, its registration and its two tests per enum were not written.
The fallback to `null` went with them: it would have turned a forgotten migration into
a rating or list status lost without a sign, where Room's converter fails the query
that reads the row. A test of a handful of lines per enum stood in for the check no
tool made, and a deliberate rename had to update it, which is the moment it asked
for the migration. Storing names still tied the stored value to the constant's
spelling; a stable code per constant, independent of its name, was considered and
left out as more code than three lists and two ratings needed.
