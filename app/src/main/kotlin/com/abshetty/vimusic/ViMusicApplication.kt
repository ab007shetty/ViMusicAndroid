package com.abshetty.vimusic

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.SingletonImageLoader
import com.abshetty.vimusic.core.datastore.SettingsStore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ViMusicApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var settings: SettingsStore

    private val artworkLoader by lazy { ArtworkImageLoader(this, settings) }

    override fun newImageLoader(context: coil3.PlatformContext) =
        artworkLoader.newImageLoader(context)

    fun artwork(): ArtworkImageLoader = artworkLoader

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
