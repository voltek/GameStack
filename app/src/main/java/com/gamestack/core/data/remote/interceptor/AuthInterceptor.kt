package com.gamestack.core.data.remote.interceptor

import com.gamestack.BuildConfig
import com.gamestack.core.domain.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val authRepository: AuthRepository
) : Interceptor {

    // Safe: OkHttp runs interceptors on its own dispatcher threads, never the caller's.
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authRepository.getValidAccessToken() }
            .getOrElse { throw IOException("Unable to obtain IGDB access token", it) }

        val authenticatedRequest = chain.request().newBuilder()
            .addHeader("Client-ID", BuildConfig.IGDB_CLIENT_ID)
            .addHeader("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
