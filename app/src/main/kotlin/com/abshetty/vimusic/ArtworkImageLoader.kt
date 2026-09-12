package com.abshetty.vimusic

import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.crossfade
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toOkioPath

class ArtworkImageLoader(
    private val context: Context,
    private val settings: com.abshetty.vimusic.core.datastore.SettingsStore,
) : SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val keepOnDisk = runCatching {
            runBlocking { settings.imageCacheEnabled.first() }
        }.getOrDefault(true)

        return ImageLoader.Builder(context)

            .components { add(EmbeddedArtFetcher.Factory()) }
            .memoryCache {
                MemoryCache.Builder()

                    .maxSizePercent(context, 0.20)
                    .build()
            }

            .diskCache {
                if (!keepOnDisk) null
                else DiskCache.Builder()
                    .directory(cacheDir())
                    .maxSizeBytes(IMAGE_CACHE_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    fun cacheDir() = context.cacheDir.resolve(DIRECTORY).toOkioPath()

    fun cacheBytes(): Long = runCatching {
        context.cacheDir.resolve(DIRECTORY).walkBottomUp()
            .filter { it.isFile }
            .sumOf { it.length() }
    }.getOrDefault(0L)

    fun clear() {
        runCatching { context.cacheDir.resolve(DIRECTORY).deleteRecursively() }
    }

    private companion object {
        const val DIRECTORY = "artwork_cache"
    }
}

private const val IMAGE_CACHE_BYTES = 256L * 1024 * 1024
