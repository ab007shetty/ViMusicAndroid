package com.abshetty.vimusic.core.media

import android.content.Context
import androidx.media3.common.util.UnstableApi
import com.abshetty.vimusic.core.media.audio.dsp.EqualizerAudioProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Module
@InstallIn(SingletonComponent::class)
object MediaModule {
    @Provides @Singleton
    fun provideEqualizerProcessor() = EqualizerAudioProcessor()

    @Provides @Singleton
    fun provideFileDataSources(
        @ApplicationContext context: Context,
    ): androidx.media3.datasource.DataSource.Factory =
        androidx.media3.datasource.DefaultDataSource.Factory(context)

    @Provides @Singleton
    fun providePlayerFactory(
        @ApplicationContext context: Context,
        fileSources: androidx.media3.datasource.DataSource.Factory,
        equalizer: EqualizerAudioProcessor,
    ) = PlayerFactory(context, fileSources, equalizer)
}

@Module
@InstallIn(SingletonComponent::class)
object MediaQueueModule {
    @Provides @Singleton
    fun provideQueueEngine() = com.abshetty.vimusic.core.media.queue.QueueEngine()
}
