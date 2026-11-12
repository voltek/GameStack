package com.gamestack.core.data.di

import com.gamestack.core.data.repository.GamesRepositoryImpl
import com.gamestack.core.domain.repository.GamesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// The product's Repositories, whatever source each reads: one that reads Room is no
// more NetworkModule's concern than a database module's.
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindGamesRepository(impl: GamesRepositoryImpl): GamesRepository
}
