package com.abshetty.vimusic.core.media

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackUiState(
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val artworkUri: String? = null,

    val previousArtworkUri: String? = null,
    val nextArtworkUri: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val hasQueue: Boolean = false,

    val error: String? = null,
)

@Singleton
class MusicServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    fun connect() {
        if (_controller.value != null) return
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val controller = future.get()
            controller.addListener(StateListener(controller))
            _controller.value = controller
            publish(controller)
        }, MoreExecutors.directExecutor())
    }

    fun release() {
        _controller.value?.release()
        _controller.value = null
    }

    fun play(items: List<MediaItem>, startIndex: Int) {
        val controller = _controller.value ?: return
        controller.setMediaItems(items, startIndex.coerceIn(0, maxOf(0, items.lastIndex)), 0L)
        controller.prepare()
        controller.play()
    }

    fun addToQueue(item: MediaItem) { _controller.value?.addMediaItem(item) }

    fun removeFromQueue(mediaId: String) {
        val controller = _controller.value ?: return
        for (index in controller.mediaItemCount - 1 downTo 0) {
            if (controller.getMediaItemAt(index).mediaId == mediaId) {
                controller.removeMediaItem(index)
            }
        }
        publish(controller)
    }

    fun refreshPosition() { _controller.value?.let(::publish) }

    private inner class StateListener(private val controller: MediaController) : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(controller)

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            android.util.Log.e("ViMusicPlayback", "player error", error)

            _state.value = _state.value.copy(
                error = error.errorCodeName + ": " + (error.cause?.message ?: error.message),
                isPlaying = false,
            )
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }

    fun stop() {
        _controller.value?.run { pause(); clearMediaItems() }
        _state.value = PlaybackUiState()
    }

    private fun publish(controller: MediaController) {
        _state.value = PlaybackUiState(
            mediaId = controller.currentMediaItem?.mediaId,
            title = controller.mediaMetadata.title?.toString().orEmpty(),
            artist = controller.mediaMetadata.artist?.toString().orEmpty(),
            artworkUri = controller.mediaMetadata.artworkUri?.toString(),
            previousArtworkUri = controller.neighbourArtwork(-1),
            nextArtworkUri = controller.neighbourArtwork(+1),
            isPlaying = controller.isPlaying,
            positionMs = controller.currentPosition.coerceAtLeast(0),

            durationMs = controller.duration.coerceAtLeast(0),
            shuffle = controller.shuffleModeEnabled,
            repeatMode = controller.repeatMode,
            hasQueue = controller.mediaItemCount > 0,
            error = _state.value.error,
        )
    }

    private fun MediaController.neighbourArtwork(delta: Int): String? {
        if (shuffleModeEnabled) return null
        val index = currentMediaItemIndex + delta
        if (index < 0 || index >= mediaItemCount) return null
        return runCatching {
            getMediaItemAt(index).mediaMetadata.artworkUri?.toString()
        }.getOrNull()
    }
}
