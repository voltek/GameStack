package com.gamestack.core.data.remote.api

import com.gamestack.core.data.remote.dto.GameDto
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST

// Apicalypse endpoints get added here via the new-api-service skill. Every call on
// this service is authenticated automatically by AuthInterceptor — never add
// Client-ID/Authorization headers manually to a function here.
interface IgdbApiService {

    @POST("games")
    suspend fun searchGames(@Body query: RequestBody): List<GameDto>
}

private const val SEARCH_GAMES_RESULT_LIMIT = 20

// IGDB search doesn't support `sort` combined with `search` — relevance ranking
// is IGDB's own, so no sort clause is ever added here.
internal fun buildSearchGamesQuery(query: String): RequestBody {
    val escapedQuery = query.replace("\\", "\\\\").replace("\"", "\\\"")
    val apicalypseQuery = """
        search "$escapedQuery";
        fields id, name, cover.url, genres.name, involved_companies.developer, involved_companies.company.name;
        limit $SEARCH_GAMES_RESULT_LIMIT;
    """.trimIndent()
    return apicalypseQuery.toApicalypseRequestBody()
}
