package com.abshetty.vimusic.core.innertube

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class InnertubeHttp

@Module
@InstallIn(SingletonComponent::class)
object InnertubeNetworkModule {
    @Provides @Singleton @InnertubeHttp
    fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
    }

    @Provides @Singleton
    fun provideInnertubeClient(@InnertubeHttp http: HttpClient) = InnertubeClient(http)

    @Provides @Singleton
    fun provideSearchService(client: InnertubeClient) = SearchService(client)
}
