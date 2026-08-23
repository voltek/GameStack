package com.gamestack.core.data.repository

import com.gamestack.core.data.remote.api.IgdbApiService
import com.gamestack.core.data.remote.dto.GameDto
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GamesRepositoryImplTest {

    private val igdbApiService: IgdbApiService = mockk()
    private val repository = GamesRepositoryImpl(igdbApiService)

    // Spec: a successful API call maps every DTO to its domain model.
    @Test
    fun `searchGames should return mapped games when api call succeeds`() = runTest {
        coEvery { igdbApiService.searchGames(any()) } returns listOf(GameDto(id = 1, name = "Elden Ring"))

        val result = repository.searchGames("Elden Ring")

        assertTrue(result.isSuccess)
        assertEquals("Elden Ring", result.getOrNull()?.first()?.name)
    }

    // Spec: a network failure surfaces as Result.failure, never an unhandled exception.
    @Test
    fun `searchGames should return failure when api call throws`() = runTest {
        coEvery { igdbApiService.searchGames(any()) } throws IOException("network down")

        val result = repository.searchGames("Elden Ring")

        assertTrue(result.isFailure)
    }
}
