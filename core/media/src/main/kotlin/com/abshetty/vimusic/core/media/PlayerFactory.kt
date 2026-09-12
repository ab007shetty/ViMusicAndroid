package com.abshetty.vimusic.core.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.abshetty.vimusic.core.media.audio.dsp.EqualizerAudioProcessor
import com.abshetty.vimusic.core.model.LocalId
import com.abshetty.vimusic.core.model.Song

object MediaIds {
    const val SCHEME = "vimusic"

    fun uriFor(videoId: String): Uri = Uri.parse(SCHEME + "://stream/" + videoId)

    fun toMediaItem(song: Song): MediaItem = MediaItem.Builder()
        .setMediaId(song.id)
        .setUri(
            if (LocalId.isLocal(song.id)) Uri.parse(LocalId.uriOf(song.id))
            else uriFor(song.id)
        )
        .setCustomCacheKey(song.id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artistsText ?: "Unknown Artist")
                .setAlbumTitle("ViMusic")

                .setArtworkUri(
                    com.abshetty.vimusic.core.model.Artwork.at(song.thumbnailUrl)?.let(Uri::parse)
                )
                .setIsPlayable(true)
                .build()
        )
        .build()
}

@UnstableApi
class PlayerFactory constructor(
    private val context: Context,

    private val fileSources: androidx.media3.datasource.DataSource.Factory,
    private val equalizer: EqualizerAudioProcessor,
) {
    private fun renderersFactory() = object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean,
        ): AudioSink = DefaultAudioSink.Builder(context)
            .setAudioProcessors(arrayOf<AudioProcessor>(equalizer))

            .setEnableFloatOutput(false)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .build()
    }.setEnableDecoderFallback(true)

    fun create(): ExoPlayer = ExoPlayer.Builder(context)
        .setRenderersFactory(renderersFactory())
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(fileSources)
        )
        .setAudioAttributes(audioAttributes(spatialize = true),  true)

        .setHandleAudioBecomingNoisy(true)

        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()

    companion object {
        fun audioAttributes(spatialize: Boolean): AudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setSpatializationBehavior(
                if (spatialize) C.SPATIALIZATION_BEHAVIOR_AUTO
                else C.SPATIALIZATION_BEHAVIOR_NEVER
            )
            .build()
    }
}
