package com.gamestack.core.data.repository

import com.gamestack.core.data.remote.api.TwitchAuthApiService
import com.gamestack.core.data.remote.dto.TwitchTokenResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {

    private val twitchAuthApiService: TwitchAuthApiService = mockk()
    private val repository = AuthRepositoryImpl(twitchAuthApiService)

    // Spec: fetches and caches a token when none exists yet.
    @Test
    fun `getValidAccessToken should fetch token from api when no token is cached`() = runTest {
        coEvery { twitchAuthApiService.getAccessToken(any(), any(), any()) } returns
            TwitchTokenResponseDto(accessToken = "abc", expiresIn = 3600, tokenType = "bearer")

        val result = repository.getValidAccessToken()

        assertEquals("abc", result.getOrNull())
        coVerify(exactly = 1) { twitchAuthApiService.getAccessToken(any(), any(), any()) }
    }

    // Spec: rest of the code never worries about expiry — a valid cached token is reused.
    @Test
    fun `getValidAccessToken should return cached token without a new api call when not expired`() = runTest {
        coEvery { twitchAuthApiService.getAccessToken(any(), any(), any()) } returns
            TwitchTokenResponseDto(accessToken = "abc", expiresIn = 3600, tokenType = "bearer")

        val first = repository.getValidAccessToken()
        val second = repository.getValidAccessToken()

        assertEquals("abc", first.getOrNull())
        assertEquals("abc", second.getOrNull())
        coVerify(exactly = 1) { twitchAuthApiService.getAccessToken(any(), any(), any()) }
    }

    // Spec: token renews automatically once expired, transparently to the caller.
    @Test
    fun `getValidAccessToken should refresh token automatically when cached token is expired`() = runTest {
        coEvery { twitchAuthApiService.getAccessToken(any(), any(), any()) } returnsMany listOf(
            TwitchTokenResponseDto(accessToken = "expired", expiresIn = 0, tokenType = "bearer"),
            TwitchTokenResponseDto(accessToken = "refreshed", expiresIn = 3600, tokenType = "bearer")
        )

        val first = repository.getValidAccessToken()
        val second = repository.getValidAccessToken()

        assertEquals("expired", first.getOrNull())
        assertEquals("refreshed", second.getOrNull())
        coVerify(exactly = 2) { twitchAuthApiService.getAccessToken(any(), any(), any()) }
    }

    @Test
    fun `getValidAccessToken should return failure when api call throws`() = runTest {
        coEvery { twitchAuthApiService.getAccessToken(any(), any(), any()) } throws IOException("network down")

        val result = repository.getValidAccessToken()

        assertTrue(result.isFailure)
    }
}
