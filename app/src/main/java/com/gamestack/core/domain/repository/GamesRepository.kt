package com.gamestack.core.domain.repository

import com.gamestack.core.domain.model.Game

interface GamesRepository {

    suspend fun searchGames(query: String): Result<List<Game>>
}
