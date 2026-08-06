package com.gamestack.core.data.repository

import com.gamestack.BuildConfig
import com.gamestack.core.data.remote.api.TwitchAuthApiService
import com.gamestack.core.domain.repository.AuthRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val twitchAuthApiService: TwitchAuthApiService
) : AuthRepository {

    private val mutex = Mutex()
    private var cachedToken: CachedToken? = null

    override suspend fun getValidAccessToken(): Result<String> = mutex.withLock {
        cachedToken?.takeIf { !it.isExpired() }?.let { return@withLock Result.success(it.accessToken) }

        runCatching {
            twitchAuthApiService.getAccessToken(
                clientId = BuildConfig.IGDB_CLIENT_ID,
                clientSecret = BuildConfig.IGDB_CLIENT_SECRET
            )
        }.map { response ->
            val token = CachedToken(
                accessToken = response.accessToken,
                expiresAtMillis = System.currentTimeMillis() + response.expiresIn * 1_000L
            )
            cachedToken = token
            token.accessToken
        }
    }

    private data class CachedToken(val accessToken: String, val expiresAtMillis: Long) {
        // Refresh a bit early so an in-flight request never races the real expiry.
        fun isExpired(): Boolean = System.currentTimeMillis() >= expiresAtMillis - EXPIRY_BUFFER_MILLIS
    }

    private companion object {
        const val EXPIRY_BUFFER_MILLIS = 60_000L
    }
}
