package com.abshetty.vimusic.core.media.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.Spatializer
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SpatialState(

    val supported: Boolean = false,

    val available: Boolean = false,

    val enabled: Boolean = true,
    val reason: String = "",
)

@Singleton
class SpatialAudio @Inject constructor(@ApplicationContext context: Context) {
    private val spatializer: Spatializer? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(AudioManager::class.java)?.spatializer
        } else {
            null
        }

    private val _state = MutableStateFlow(SpatialState())
    val state: StateFlow<SpatialState> = _state.asStateFlow()

    init {
        refresh(_state.value.enabled)
    }

    fun setEnabled(enabled: Boolean) = refresh(enabled)

    fun refresh(enabled: Boolean = _state.value.enabled) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || spatializer == null) {
            _state.value = SpatialState(
                supported = false,
                available = false,
                enabled = enabled,
                reason = "Needs Android 13 or newer",
            )
            return
        }

        val level = spatializer.immersiveAudioLevel
        val supported = level != Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE

        val available = supported && spatializer.isEnabled && spatializer.isAvailable

        _state.value = SpatialState(
            supported = supported,
            available = available,
            enabled = enabled,
            reason = when {
                !supported -> "This device has no spatial audio engine"
                !spatializer.isEnabled -> "Turn on spatial audio in system sound settings"
                !spatializer.isAvailable -> "Not available on the current output device"
                else -> "Active on this output"
            },
        )
    }

    fun canSpatializeStereo(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val s = spatializer ?: return false
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .setSampleRate(48_000)
            .build()
        return runCatching { s.canBeSpatialized(attributes, format) }.getOrDefault(false)
    }
}
