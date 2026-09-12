package com.abshetty.vimusic

import android.media.MediaMetadataRetriever
import android.net.Uri as AndroidUri
import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer

class EmbeddedArtFetcher(
    private val data: Uri,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val uri = AndroidUri.parse(data.toString())
        val picture = runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(options.context, uri)
                retriever.embeddedPicture
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrNull() ?: return@withContext null

        if (picture.isEmpty()) return@withContext null

        SourceFetchResult(
            source = ImageSource(
                source = Buffer().apply { write(picture) },
                fileSystem = options.fileSystem,
            ),
            mimeType = null,

            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != "content") return null

            val type = runCatching {
                options.context.contentResolver.getType(AndroidUri.parse(data.toString()))
            }.getOrNull().orEmpty()

            val looksAudio = type.startsWith("audio/") ||
                type == "application/ogg" ||
                data.path?.substringAfterLast('.', "")?.lowercase() in AUDIO_EXTENSIONS

            return if (looksAudio) EmbeddedArtFetcher(data, options) else null
        }

        private companion object {
            val AUDIO_EXTENSIONS = setOf(
                "mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "wma", "mka", "aiff", "alac",
            )
        }
    }
}
