package com.abshetty.vimusic.core.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class VideoState(
    val enabled: Boolean = false,
    val captionsEnabled: Boolean = false,
)

@Singleton
class VideoModeController @Inject constructor() {
    private val _state = MutableStateFlow(VideoState())
    val state: StateFlow<VideoState> = _state.asStateFlow()

    fun onTrackChanged() = Unit

    @Suppress("UNUSED_PARAMETER")
    suspend fun setEnabled(enabled: Boolean, videoId: String?) {
        _state.value = _state.value.copy(enabled = enabled)
    }

    fun toggleCaptions() {
        _state.value = _state.value.copy(captionsEnabled = !_state.value.captionsEnabled)
    }
}
