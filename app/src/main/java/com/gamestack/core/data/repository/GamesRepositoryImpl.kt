package com.gamestack.core.data.repository

import com.gamestack.core.data.mapper.toDomain
import com.gamestack.core.data.remote.api.IgdbApiService
import com.gamestack.core.data.remote.api.buildSearchGamesQuery
import com.gamestack.core.domain.model.Game
import com.gamestack.core.domain.repository.GamesRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class GamesRepositoryImpl @Inject constructor(
    private val igdbApiService: IgdbApiService
) : GamesRepository {

    // try/catch, not runCatching — runCatching also swallows CancellationException,
    // turning a debounce-cancelled search into a false error (see CLAUDE.md).
    override suspend fun searchGames(query: String): Result<List<Game>> =
        try {
            Result.success(igdbApiService.searchGames(buildSearchGamesQuery(query)).map { it.toDomain() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
