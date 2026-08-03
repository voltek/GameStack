package com.gamestack.core.data.remote.interceptor

import com.gamestack.BuildConfig
import com.gamestack.core.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthInterceptorTest {

    private val authRepository: AuthRepository = mockk()
    private val interceptor = AuthInterceptor(authRepository)

    private val request = Request.Builder().url("https://api.igdb.com/v4/games").build()

    private fun fakeResponse(request: Request) = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .build()

    // Spec: adds Client-ID and Authorization headers automatically when the token is valid.
    @Test
    fun `intercept should add Client-ID and Authorization headers when token is valid`() {
        coEvery { authRepository.getValidAccessToken() } returns Result.success("test-token")

        val chain = mockk<Interceptor.Chain>()
        val requestSlot = slot<Request>()
        every { chain.request() } returns request
        every { chain.proceed(capture(requestSlot)) } answers { fakeResponse(requestSlot.captured) }

        interceptor.intercept(chain)

        val sentRequest = requestSlot.captured
        assertEquals(BuildConfig.IGDB_CLIENT_ID, sentRequest.header("Client-ID"))
        assertEquals("Bearer test-token", sentRequest.header("Authorization"))
    }

    // Spec: never proceeds with an unauthenticated request — fails loudly instead.
    @Test
    fun `intercept should throw IOException when getValidAccessToken fails`() {
        coEvery { authRepository.getValidAccessToken() } returns
            Result.failure(RuntimeException("token fetch failed"))

        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request

        assertThrows(IOException::class.java) {
            interceptor.intercept(chain)
        }
    }
}
