package com.gamestack.feature.search.domain.usecase

import com.gamestack.core.domain.model.Game
import com.gamestack.core.domain.repository.GamesRepository
import javax.inject.Inject

class SearchGamesUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {

    // Blank queries never reach IGDB — there's nothing to search for and no
    // point spending an API call on it.
    suspend operator fun invoke(query: String): Result<List<Game>> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return Result.success(emptyList())
        return gamesRepository.searchGames(trimmedQuery)
    }
}
