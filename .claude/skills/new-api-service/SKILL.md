---
name: new-api-service
description: Create a Retrofit interface for the IGDB API using Apicalypse query syntax. Use when asked to create or add an endpoint to the IGDB API service.
---

## Critical context
IGDB does NOT use standard REST. It uses Apicalypse: every request is a POST
with the query written as plain text in the request body — not GET with query params.

Example real request:
```
POST https://api.igdb.com/v4/games
Body: fields id, name, rating; where platforms=48; limit 10;
```

## Construction
- Interface annotated for Retrofit, using `@POST("{endpoint}")` — never `@GET`.
- The Apicalypse query string goes in the request body via `@Body` (as plain text/RequestBody,
  not a JSON object).
- Every function is `suspend fun` returning `List<{Dto}>`.
- Requires two headers on every call: `Client-ID` and `Authorization: Bearer {token}`
  (see AuthRepository for token handling — do not hardcode).

## Location
`core/data/remote/api/IgdbApiService.kt` (shared — all features consume the same IGDB API).

## Naming convention
Function name describes the query intent, not the endpoint mechanically.
Example: `searchGames(query: String)`, `getPopularGames()`.

## Quality criteria
- Never use `@GET` or standard query annotations (`@Query`) for IGDB calls.
- The Apicalypse query string itself can be built as a constant or a small
  private builder function — do not inline complex queries repeatedly.
- After creating or modifying the API service, invoke the `write-tests` skill
  if any query-building logic was added (pure Retrofit interfaces don't need tests themselves).
