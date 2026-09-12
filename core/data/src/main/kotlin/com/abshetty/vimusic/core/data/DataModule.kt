package com.abshetty.vimusic.core.data

import io.ktor.serialization.kotlinx.json.json

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

@Module
@InstallIn(SingletonComponent::class)
object OutboxModule {
    @Provides @Singleton
    fun provideOutboxWriter(
        dao: com.abshetty.vimusic.core.database.dao.OutboxDao,
    ) = com.abshetty.vimusic.core.data.sync.OutboxWriter(dao)
}

@Module
@InstallIn(SingletonComponent::class)
object HttpModule {
    @Provides @Singleton
    fun provideHttpClient(): io.ktor.client.HttpClient =
        io.ktor.client.HttpClient(io.ktor.client.engine.okhttp.OkHttp) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(
                    kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
                )
            }
            install(io.ktor.client.plugins.HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 8_000
            }
        }
}
