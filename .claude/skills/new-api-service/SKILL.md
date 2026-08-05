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

IMPORTANT — search does not support sorting: the `search "query";` keyword
cannot be combined with `sort` in the same query. IGDB returns search results
ordered by its own relevance ranking. Do not attempt to add `sort` to a query
that also uses `search` — it will not behave as expected.

## Construction
- Interface annotated for Retrofit, using `@POST("{endpoint}")` — never `@GET`.
- The Apicalypse query string goes in the request body via `@Body` (as plain text/RequestBody,
  not a JSON object).
- Every function is `suspend fun` returning `List<{Dto}>`.
- **Do NOT declare authentication headers on the function.** IGDB requires
  `Client-ID` and `Authorization: Bearer {token}` on every call, but `AuthInterceptor`
  attaches both automatically to every request on the IGDB OkHttp client. Adding
  `@Header("Client-ID") clientId: String` (or `@Headers(...)`) is wrong here: it
  duplicates the header, forces every caller to thread a credential it shouldn't
  know about, and contradicts CLAUDE.md's rule that the token is never attached
  manually. Functions take query parameters only.

## Location
`core/data/remote/api/IgdbApiService.kt` (shared — all features consume the same IGDB API).

## Naming convention
Function name describes the query intent, not the endpoint mechanically.
Example: `searchGames(query: String)`, `getPopularGames()`.

## Quality criteria
- Never use `@GET` or standard query annotations (`@Query`) for IGDB calls.
- No `@Header`/`@Headers` annotation for auth anywhere in the interface.
- New endpoints are added to the existing `IgdbApiService` — do not create a
  second IGDB service interface. Its Retrofit instance is already bound to the
  authenticated OkHttp client; a new one would silently bypass `AuthInterceptor`.
- The Apicalypse query string itself can be built as a constant or a small
  private builder function — do not inline complex queries repeatedly.
- After creating or modifying the API service, invoke the `write-tests` skill
  if any query-building logic was added (pure Retrofit interfaces don't need tests themselves).
