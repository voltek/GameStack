# new-api-service Skill

Source: `.claude/skills/new-api-service/SKILL.md`

## Purpose
Creates a Retrofit interface for the IGDB API, using Apicalypse query syntax instead of standard REST.

## Key design decisions
- IGDB requests are always `@POST`, never `@GET` — the query is plain text in the request body via `@Body`, not JSON.
- Every call requires `Client-ID` and `Authorization: Bearer {token}` headers (token handled by AuthRepository, never hardcoded).
- This is the concrete implementation of the Apicalypse discovery documented in Block 1.

## Iteration history
- v1: initial version