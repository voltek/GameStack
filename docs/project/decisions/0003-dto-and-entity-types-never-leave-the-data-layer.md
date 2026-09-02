# ADR 0003 — `toEntity()` is legitimate; DTO and Entity types never leave Data

Status: Accepted
Date: 2026-08-04
Backfilled: yes — DRIFT-LOG.md, row 2026-08-04 ("A decision with no incident behind it: the write-path conversion")
Enforced by: checklist detector — DRIFT-CHECKLIST.md § Code Conventions

## Context
Mappers were documented only as the read path — `DtoOrEntity.toDomain()` — which
left the write path with no defined home. Every feature that persists something
(saving a rating, moving a game between lists) has to convert a Domain model
into a Room Entity somewhere, and with only the read path written down that
conversion had nowhere legitimate to live. The underlying rule had been stated
as a direction of travel, which is what made the write path look like a
violation of it.

## Decision
The rule is about **where types surface**, not which way a mapper runs: a DTO or
Entity type must never appear in a signature reachable from the Domain or UI
layer. Both directions are therefore legitimate and both live in the data layer.

- Read path — `DtoOrEntity.toDomain()`: inbound data becomes a Domain model.
- Write path — `DomainModel.toEntity()`: its *input* is a Domain model and its
  *output* never leaves the data layer, so the boundary holds.

Still forbidden: a mapper returning a DTO or Entity to a Domain or UI caller,
and any DTO→Entity mapper — remote and local models must not know about each
other, and convert through the Domain model instead.

## Consequences
The write features have a defined home for their conversions, and the boundary
is stated as something checkable — search for the types in signatures — rather
than as a direction that has to be argued about case by case.

The cost: two mapper directions to keep straight, and a same-layer
transformation (building an updated copy of an Entity to persist) now has to be
recognised as *not* a mapper at all, which is a distinction someone has to hold.
