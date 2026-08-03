package com.gamestack.core.data.di

import com.gamestack.core.data.remote.api.IgdbApiService
import com.gamestack.core.data.remote.api.TwitchAuthApiService
import com.gamestack.core.data.remote.interceptor.AuthInterceptor
import com.gamestack.core.data.repository.AuthRepositoryImpl
import com.gamestack.core.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

private const val IGDB_BASE_URL = "https://api.igdb.com/v4/"
private const val TWITCH_AUTH_BASE_URL = "https://id.twitch.tv/"

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    companion object {

        // Not @Provides: only used internally to build the Retrofit converter below —
        // nothing outside this module needs to inject Json directly.
        private fun createJsonConverterFactory(): Converter.Factory {
            val json = Json { ignoreUnknownKeys = true }
            return json.asConverterFactory("application/json".toMediaType())
        }

        // No AuthInterceptor here: fetching the Twitch token can't depend on itself.
        @Provides
        @Singleton
        @TwitchAuthHttpClient
        fun provideTwitchAuthHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

        @Provides
        @Singleton
        @IgdbHttpClient
        fun provideIgdbHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
            OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .build()

        @Provides
        @Singleton
        @TwitchAuthRetrofit
        fun provideTwitchAuthRetrofit(
            @TwitchAuthHttpClient client: OkHttpClient
        ): Retrofit = Retrofit.Builder()
            .baseUrl(TWITCH_AUTH_BASE_URL)
            .client(client)
            .addConverterFactory(createJsonConverterFactory())
            .build()

        @Provides
        @Singleton
        @IgdbRetrofit
        fun provideIgdbRetrofit(
            @IgdbHttpClient client: OkHttpClient
        ): Retrofit = Retrofit.Builder()
            .baseUrl(IGDB_BASE_URL)
            .client(client)
            .addConverterFactory(createJsonConverterFactory())
            .build()

        @Provides
        @Singleton
        fun provideTwitchAuthApiService(@TwitchAuthRetrofit retrofit: Retrofit): TwitchAuthApiService =
            retrofit.create(TwitchAuthApiService::class.java)

        @Provides
        @Singleton
        fun provideIgdbApiService(@IgdbRetrofit retrofit: Retrofit): IgdbApiService =
            retrofit.create(IgdbApiService::class.java)
    }
}
