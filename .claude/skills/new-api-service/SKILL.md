---
name: new-api-service
description: Create a Retrofit interface for the IGDB API using Apicalypse query syntax. Use when asked to create or add an endpoint to the IGDB API service.
---

## How IGDB differs from REST
IGDB uses Apicalypse: every request is a POST whose body is the query as plain text,
never a GET with query parameters.

```
POST https://api.igdb.com/v4/games
Body: fields id, name, cover.url; where id = 1942;
```

A response is always a JSON array, even for one id, so every function returns
`List<{Dto}>` and the Repository takes the element it needs.

Search does not combine with sorting: a query using `search "…";` carries no `sort`,
and the results come in IGDB's relevance order (the Search spec, checked by
`IgdbApiServiceQueryBuilderTest`).

## Construction
- Add the function to the existing `IgdbApiService`
  (`core/data/remote/api/IgdbApiService.kt`). Its Retrofit instance sits on the OkHttp
  client that carries `AuthInterceptor`, so a second IGDB service would send requests
  with no credentials.
- `@POST("{endpoint}") suspend fun name(@Body query: RequestBody): List<{Dto}>`. No
  `@Header` for `Client-ID` or `Authorization`: `AuthInterceptor` adds both.
- Build the query in an `internal` top-level function beside the interface,
  `build{Name}Query(...)`, returning `String.toApicalypseRequestBody()`. It is
  `internal` rather than private so its test can read the body it builds.
- Text the user typed goes into an Apicalypse string literal, so escape `\` first and
  then `"`, as `buildSearchGamesQuery` does; otherwise a quote ends the literal and
  rewrites the query. A number, such as an id, needs no escaping.
- Name the function for what the query asks, not the endpoint: `searchGames`,
  `getGameDetails`.

## The DTO
`@Serializable` in `core/data/remote/dto/`, one class per object the response nests
(`GameDto`, `GameCoverDto`). A field IGDB may leave out is nullable with `= null`:
deserialization fails on a missing field that has no default, and one such game fails
the whole list. Only a game's `id` and `name` are required, since a game without a
name is not one the app can show; whether a nested object's `name` may be missing is
#203. `@SerialName` maps a snake_case field (`involved_companies`).
Request only the fields the DTO declares: `fields` decides what comes back, and
`Json { ignoreUnknownKeys = true }` (in `NetworkModule`) skips the rest.

## Tests
A query builder gets a test in `IgdbApiServiceQueryBuilderTest` (`write-tests`): the
clauses it must hold, the escaping if it takes text, and what it must never contain.
The Retrofit interface itself needs none.
