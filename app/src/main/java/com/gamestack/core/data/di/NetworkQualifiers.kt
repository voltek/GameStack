package com.gamestack.core.data.di

import javax.inject.Qualifier

// Grouped together: each only has meaning paired with NetworkModule's two client/Retrofit stacks.
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IgdbHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IgdbRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TwitchAuthHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TwitchAuthRetrofit
