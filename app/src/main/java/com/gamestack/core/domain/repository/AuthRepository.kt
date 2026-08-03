package com.gamestack.core.domain.repository

interface AuthRepository {

    // Caller never deals with expiry — a cached token is reused or refreshed transparently.
    suspend fun getValidAccessToken(): Result<String>
}
