package com.abshetty.vimusic.core.media.embedded

import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.abshetty.vimusic.core.model.LocalId
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@UnstableApi
class EmbeddedPlayer(
    private val host: YouTubeIFrameHost,
    private val scope: CoroutineScope,
) : SimpleBasePlayer(Looper.getMainLooper()) {
    private var playlist: List<MediaItem> = emptyList()
    private var index = 0
    private var playWhenReady = false
    private var repeatMode = Player.REPEAT_MODE_OFF
    private var shuffle = false
    private var volume = 1f
    private var released = false

    init {
        host.load()
        scope.launch {
            var wasEnded = false
            host.status.collect { status ->
                val ended = status.state == EmbeddedState.ENDED
                if (ended && !wasEnded) onTrackFinished()
                wasEnded = ended
                invalidateState()
            }
        }
    }

    private fun onTrackFinished() {
        when {
            repeatMode == Player.REPEAT_MODE_ONE -> startCurrent(0)
            index < playlist.lastIndex -> { index++; startCurrent(0) }
            repeatMode == Player.REPEAT_MODE_ALL && playlist.isNotEmpty() -> {
                index = 0
                startCurrent(0)
            }
            else -> playWhenReady = false
        }
    }

    private fun startCurrent(positionMs: Long) {
        val item = playlist.getOrNull(index) ?: return
        val id = item.mediaId
        if (LocalId.isLocal(id)) {
            if (index < playlist.lastIndex) { index++; startCurrent(0) } else playWhenReady = false
            return
        }
        host.play(id, positionMs)
        if (!playWhenReady) host.pause()
    }

    override fun getState(): State {
        val status = host.status.value
        val playbackState = when {
            playlist.isEmpty() -> Player.STATE_IDLE
            status.state == EmbeddedState.BUFFERING -> Player.STATE_BUFFERING
            status.state == EmbeddedState.ENDED && index >= playlist.lastIndex -> Player.STATE_ENDED
            status.ready -> Player.STATE_READY
            else -> Player.STATE_BUFFERING
        }

        return State.Builder()
            .setAvailableCommands(COMMANDS)
            .setPlaybackState(playbackState)
            .setPlayWhenReady(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setRepeatMode(repeatMode)
            .setShuffleModeEnabled(shuffle)
            .setVolume(volume)
            .setPlaylist(
                playlist.mapIndexed { position, item ->
                    MediaItemData.Builder(item.mediaId + "#" + position)
                        .setMediaItem(item)
                        .setDurationUs(
                            if (position == index && status.durationMs > 0) {
                                status.durationMs * 1000
                            } else {
                                C.TIME_UNSET
                            }
                        )
                        .build()
                }
            )
            .setCurrentMediaItemIndex(index.coerceIn(0, maxOf(0, playlist.lastIndex)))
            .setContentPositionMs { host.status.value.positionMs }
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        this.playWhenReady = playWhenReady
        if (playWhenReady) host.resume() else host.pause()
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> = Futures.immediateVoidFuture()

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int,
    ): ListenableFuture<*> {
        val target = mediaItemIndex.coerceIn(0, maxOf(0, playlist.lastIndex))
        val within = target == index
        index = target
        val at = if (positionMs == C.TIME_UNSET) 0 else positionMs

        if (within && seekCommand != Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM &&
            seekCommand != Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
        ) {
            host.seekTo(at)
        } else {
            startCurrent(at)
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<*> {
        playlist = mediaItems
        index = if (startIndex == C.INDEX_UNSET) 0 else startIndex.coerceIn(0, maxOf(0, mediaItems.lastIndex))
        startCurrent(if (startPositionMs == C.TIME_UNSET) 0 else startPositionMs)
        return Futures.immediateVoidFuture()
    }

    override fun handleAddMediaItems(index: Int, mediaItems: List<MediaItem>): ListenableFuture<*> {
        playlist = playlist.toMutableList().apply { addAll(index.coerceIn(0, size), mediaItems) }
        return Futures.immediateVoidFuture()
    }

    override fun handleRemoveMediaItems(fromIndex: Int, toIndex: Int): ListenableFuture<*> {
        val list = playlist.toMutableList()
        val from = fromIndex.coerceIn(0, list.size)
        val to = toIndex.coerceIn(from, list.size)
        val removingCurrent = index in from until to
        list.subList(from, to).clear()
        playlist = list
        if (removingCurrent) {
            index = from.coerceAtMost(maxOf(0, list.lastIndex))
            if (list.isEmpty()) host.stop() else startCurrent(0)
        } else if (index >= to) {
            index -= (to - from)
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleMoveMediaItems(
        fromIndex: Int,
        toIndex: Int,
        newIndex: Int,
    ): ListenableFuture<*> {
        val list = playlist.toMutableList()
        val moved = list.subList(fromIndex, toIndex).toList()
        list.subList(fromIndex, toIndex).clear()
        list.addAll(newIndex.coerceIn(0, list.size), moved)
        playlist = list
        return Futures.immediateVoidFuture()
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
        this.repeatMode = repeatMode
        return Futures.immediateVoidFuture()
    }

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
        shuffle = shuffleModeEnabled
        return Futures.immediateVoidFuture()
    }

    override fun handleSetVolume(volume: Float): ListenableFuture<*> {
        this.volume = volume
        host.setVolume(volume)
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        playWhenReady = false
        host.stop()
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        if (!released) {
            released = true
            host.release()
        }
        return Futures.immediateVoidFuture()
    }

    private companion object {
        val COMMANDS: Player.Commands = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_PREPARE,
                Player.COMMAND_STOP,
                Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_BACK,
                Player.COMMAND_SEEK_FORWARD,
                Player.COMMAND_SET_REPEAT_MODE,
                Player.COMMAND_SET_SHUFFLE_MODE,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_TIMELINE,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_CHANGE_MEDIA_ITEMS,
                Player.COMMAND_SET_MEDIA_ITEM,
                Player.COMMAND_GET_VOLUME,
                Player.COMMAND_SET_VOLUME,
                Player.COMMAND_RELEASE,
            )
            .build()
    }
}
