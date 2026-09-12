package com.abshetty.vimusic.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.abshetty.vimusic.core.data.repository.Lyrics
import com.abshetty.vimusic.core.data.repository.LyricsRepository
import com.abshetty.vimusic.core.data.repository.SongRepository
import com.abshetty.vimusic.core.media.MusicServiceConnection
import com.abshetty.vimusic.core.media.PlaybackUiState
import com.abshetty.vimusic.core.media.VideoModeController
import com.abshetty.vimusic.core.media.VideoState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.map

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val connection: MusicServiceConnection,
    private val videoMode: VideoModeController,
    private val lyricsRepo: LyricsRepository,
    private val songs: SongRepository,
    auth: com.abshetty.vimusic.core.data.auth.AuthRepository,
    private val embeddedHost: com.abshetty.vimusic.core.media.embedded.IFrameHostHolder,
) : ViewModel() {
    fun embedded() = embeddedHost.peek()

    val embeddedStatus: StateFlow<com.abshetty.vimusic.core.media.embedded.EmbeddedStatus?> =
        embeddedHost.statusOrNull

    val state: StateFlow<PlaybackUiState> = connection.state

    val isGuest: StateFlow<Boolean> = auth.userId
        .map { com.abshetty.vimusic.core.model.UserId.isGuest(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val video: StateFlow<VideoState> = videoMode.state

    val isFavourite: StateFlow<Boolean> =
        combine(connection.state, songs.favourites()) { playback, favourites ->
            playback.mediaId != null && favourites.any { it.id == playback.mediaId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _lyrics = MutableStateFlow<Lyrics?>(null)
    val lyrics: StateFlow<Lyrics?> = _lyrics.asStateFlow()

    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading: StateFlow<Boolean> = _lyricsLoading.asStateFlow()

    private var lyricsForMediaId: String? = null

    fun togglePlay() {
        val c = connection.controller.value ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { connection.controller.value?.seekToNextMediaItem() }
    fun previous() { connection.controller.value?.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) { connection.controller.value?.seekTo(ms) }
    fun refreshPosition() = connection.refreshPosition()

    fun seekBy(deltaMs: Long) {
        val c = connection.controller.value ?: return
        val duration = c.duration.coerceAtLeast(0)
        c.seekTo((c.currentPosition + deltaMs).coerceIn(0, duration))
    }

    fun toggleShuffle() {
        val c = connection.controller.value ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = connection.controller.value ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun toggleVideo() {
        val mediaId = state.value.mediaId
        viewModelScope.launch { videoMode.setEnabled(!video.value.enabled, mediaId) }
    }

    fun toggleCaptions() = videoMode.toggleCaptions()

    fun loadLyrics() {
        val s = state.value
        val mediaId = s.mediaId ?: return
        if (lyricsForMediaId == mediaId && _lyrics.value != null) return

        viewModelScope.launch {
            _lyricsLoading.value = true
            val song = songs.find(mediaId)
            val artist = s.artist.ifBlank { song?.artistsText.orEmpty() }
            _lyrics.value = lyricsRepo.lyricsFor(
                songId = mediaId,
                title = s.title.ifBlank { song?.title.orEmpty() },

                artist = artist.takeUnless { it == UNKNOWN_ARTIST },
                durationSeconds = (s.durationMs / 1000).toInt().takeIf { it > 0 },
            )
            lyricsForMediaId = mediaId
            _lyricsLoading.value = false
        }
    }

    private companion object {
        const val UNKNOWN_ARTIST = "Unknown Artist"
    }

    fun onTrackChanged() {
        _lyrics.value = null
        lyricsForMediaId = null
        videoMode.onTrackChanged()
    }

    fun queueEntries(): List<QueueEntry> {
        val c = connection.controller.value ?: return emptyList()
        return (0 until c.mediaItemCount).map { index ->
            val item = c.getMediaItemAt(index)
            QueueEntry(
                mediaId = item.mediaId,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
                artworkUri = item.mediaMetadata.artworkUri?.toString(),
                isCurrent = index == c.currentMediaItemIndex,
            )
        }
    }

    fun playQueueIndex(index: Int) {
        connection.controller.value?.seekTo(index, 0L)
        connection.controller.value?.play()
    }

    fun removeQueueIndex(index: Int) {
        connection.controller.value?.removeMediaItem(index)
        connection.refreshPosition()
    }

    fun stop() = connection.stop()

    fun clearError() = connection.clearError()

    fun toggleFavouriteCurrent() {
        val mediaId = state.value.mediaId ?: return
        viewModelScope.launch {
            songs.find(mediaId)?.let { songs.toggleFavourite(it) }
        }
    }
}
