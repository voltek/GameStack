package com.gamestack.feature.search.domain.usecase

import com.gamestack.core.domain.model.Game
import com.gamestack.core.domain.repository.GamesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchGamesUseCaseTest {

    private val gamesRepository: GamesRepository = mockk()
    private val useCase = SearchGamesUseCase(gamesRepository)

    // Spec: a blank query returns an empty result without ever calling the repository.
    @Test
    fun `invoke should return empty list without calling repository when query is blank`() = runTest {
        val result = useCase("   ")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
        coVerify(exactly = 0) { gamesRepository.searchGames(any()) }
    }

    // Spec: surrounding whitespace is trimmed before reaching the repository.
    @Test
    fun `invoke should trim query before delegating to repository`() = runTest {
        coEvery { gamesRepository.searchGames("Elden Ring") } returns Result.success(emptyList())

        useCase("  Elden Ring  ")

        coVerify(exactly = 1) { gamesRepository.searchGames("Elden Ring") }
    }

    // Spec: a non-blank query delegates to the repository and returns its result untouched.
    @Test
    fun `invoke should return repository result when query is not blank`() = runTest {
        val games = listOf(Game(id = 1, name = "Elden Ring", coverUrl = null, genres = listOf("RPG"), developer = "FromSoftware"))
        coEvery { gamesRepository.searchGames("Elden Ring") } returns Result.success(games)

        val result = useCase("Elden Ring")

        assertEquals(games, result.getOrNull())
    }

    // Spec: repository failures propagate untouched, not swallowed.
    @Test
    fun `invoke should propagate failure when repository fails`() = runTest {
        val failure = RuntimeException("network down")
        coEvery { gamesRepository.searchGames("Elden Ring") } returns Result.failure(failure)

        val result = useCase("Elden Ring")

        assertTrue(result.isFailure)
        assertEquals(failure, result.exceptionOrNull())
    }
}
