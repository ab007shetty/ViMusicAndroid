package com.abshetty.vimusic.core.media

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.abshetty.vimusic.core.data.repository.SongRepository
import com.abshetty.vimusic.core.media.queue.QueueEngine
import com.abshetty.vimusic.core.media.queue.QueueState
import com.abshetty.vimusic.core.media.queue.QueueTransition
import com.abshetty.vimusic.core.media.queue.RepeatMode
import com.abshetty.vimusic.core.model.LocalId
import com.abshetty.vimusic.core.model.Song
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaLibraryService() {
    @Inject lateinit var playerFactory: PlayerFactory
    @Inject lateinit var songRepository: SongRepository
    @Inject lateinit var queueEngine: QueueEngine
    @Inject lateinit var audioEffects: com.abshetty.vimusic.core.media.audio.AudioEffects
    @Inject lateinit var spatialAudio: com.abshetty.vimusic.core.media.audio.SpatialAudio
    @Inject lateinit var settings: com.abshetty.vimusic.core.datastore.SettingsStore

    @Inject lateinit var iframeHost: com.abshetty.vimusic.core.media.embedded.IFrameHostHolder

    private lateinit var exoPlayer: ExoPlayer

    private val embedded by lazy {
        val host = iframeHost.get()

        host.attachToOwnWindow(this)
        com.abshetty.vimusic.core.media.embedded.EmbeddedPlayer(host = host, scope = scope)
    }
    private var session: MediaLibrarySession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val stats = PlaybackStatsTracker()
    private var currentSong: Song? = null

    private var directPlayer: Player? = null

    private var playbackListener: Player.Listener? = null
    private var embeddedListening = false

    override fun onCreate() {
        super.onCreate()
        exoPlayer = playerFactory.create()

        directPlayer = exoPlayer
        val player: Player = sessionPlayer(exoPlayer)

        scope.launch {
            settings.spatialAudio.collect { enabled ->
                exoPlayer.setAudioAttributes(
                    PlayerFactory.audioAttributes(spatialize = enabled),
                     true,
                )
                spatialAudio.setEnabled(enabled)
            }
        }

        session = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .setId("vimusic")
            .build()

        audioEffects.attach(exoPlayer.audioSessionId)

        scope.launch {
            audioEffects.state.collect { fx ->
                val target = androidx.media3.common.PlaybackParameters(fx.speed, fx.pitch)
                if (exoPlayer.playbackParameters != target) {
                    exoPlayer.playbackParameters = target
                }
            }
        }

        playbackListener = object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                audioEffects.attach(audioSessionId)
            }

            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                flushPlayTime()
                currentSong = item?.let(::songFromMediaItem)
                broadcastPlaybackState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) flushPlayTime()

                if (isPlaying) audioEffects.attach(exoPlayer.audioSessionId)
                broadcastPlaybackState()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("ViMusicPlayback", PlaybackErrors.message(error), error)

                if (PlaybackErrors.shouldSkip(error) && exoPlayer.hasNextMediaItem()) {
                    scope.launch {
                        delay(SKIP_DELAY_MS)
                        exoPlayer.seekToNextMediaItem()
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }
                } else {
                    exoPlayer.pause()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state != Player.STATE_ENDED) return

                if (queueEngine.onTrackEnded(currentQueueState()) == QueueTransition.Stop) {
                    exoPlayer.pause()
                }
            }
        }
        exoPlayer.addListener(playbackListener!!)

        startTicker()
    }

    private fun startTicker() = scope.launch {
        while (true) {
            delay(TICK_MS)
            stats.onTick(TICK_MS, session?.player?.isPlaying == true)
            stats.flushIfDue()?.let(::report)
        }
    }

    private fun flushPlayTime() { stats.flushNow()?.let(::report) }

    private fun report(ms: Long) {
        val song = currentSong ?: return
        scope.launch { songRepository.recordPlayTime(song, ms) }
    }

    private fun currentQueueState() = QueueState(
        items = (0 until exoPlayer.mediaItemCount).map { exoPlayer.getMediaItemAt(it).mediaId },
        index = exoPlayer.currentMediaItemIndex,
        shuffle = exoPlayer.shuffleModeEnabled,
        repeat = when (exoPlayer.repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        },
    )

    private fun songFromMediaItem(item: MediaItem) = Song(
        id = item.mediaId,

        userId = "",
        title = item.mediaMetadata.title?.toString().orEmpty(),
        artistsText = item.mediaMetadata.artist?.toString(),
        thumbnailUrl = item.mediaMetadata.artworkUri?.toString(),
    )

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!exoPlayer.playWhenReady || exoPlayer.mediaItemCount == 0) {
            stopSelf()
        }
    }

    private fun broadcastPlaybackState() {
        val item = exoPlayer.currentMediaItem
        sendBroadcast(
            android.content.Intent(ACTION_PLAYBACK_STATE).apply {
                setPackage(packageName)
                putExtra(EXTRA_TITLE, item?.mediaMetadata?.title?.toString().orEmpty())
                putExtra(EXTRA_ARTIST, item?.mediaMetadata?.artist?.toString().orEmpty())
                putExtra(EXTRA_ARTWORK, item?.mediaMetadata?.artworkUri?.toString())
                putExtra(EXTRA_IS_PLAYING, exoPlayer.isPlaying)
            }
        )
    }

    override fun onDestroy() {
        flushPlayTime()
        audioEffects.release()
        scope.cancel()
        session?.release()
        exoPlayer.release()
        session = null
        super.onDestroy()
    }

    private fun playerFor(items: List<MediaItem>): Player {
        val allLocal = items.isNotEmpty() && items.all { LocalId.isLocal(it.mediaId) }
        if (allLocal) return sessionPlayer(exoPlayer)

        return embedded.also {
            if (!embeddedListening) {
                embeddedListening = true
                playbackListener?.let(it::addListener)
            }
        }
    }

    private fun sessionPlayer(player: ExoPlayer): Player = directPlayer ?: player

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): com.google.common.util.concurrent.ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val wanted = playerFor(mediaItems)
            if (mediaSession.player !== wanted) {
                runCatching { mediaSession.player.stop() }
                mediaSession.player = wanted
            }
            return com.google.common.util.concurrent.Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
            )
        }
    }

    companion object {
        internal const val TICK_MS = 500L

        internal const val SKIP_DELAY_MS = 1_200L

        const val ACTION_PLAYBACK_STATE = "com.abshetty.vimusic.PLAYBACK_STATE"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_ARTWORK = "artwork"
        const val EXTRA_IS_PLAYING = "isPlaying"
    }
}
