package com.abshetty.vimusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.abshetty.vimusic.MainActivity
import com.abshetty.vimusic.R
import com.abshetty.vimusic.core.media.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PlayerWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == MusicService.ACTION_PLAYBACK_STATE) {
            val title = intent.getStringExtra(MusicService.EXTRA_TITLE).orEmpty()
            publish(
                context,
                Snapshot(
                    title = title.ifBlank { "Nothing playing" },
                    artist = intent.getStringExtra(MusicService.EXTRA_ARTIST).orEmpty(),
                    artworkUri = intent.getStringExtra(MusicService.EXTRA_ARTWORK),
                    isPlaying = intent.getBooleanExtra(MusicService.EXTRA_IS_PLAYING, false),
                ),
            )
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context, WidgetState.last))
        }
    }

    companion object {
        object WidgetState {
            @Volatile var last: Snapshot = Snapshot()
        }

        data class Snapshot(
            val title: String = "Nothing playing",
            val artist: String = "",
            val artworkUri: String? = null,
            val isPlaying: Boolean = false,
        )

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        fun publish(context: Context, snapshot: Snapshot) {
            WidgetState.last = snapshot
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, PlayerWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return

            val views = buildViews(context, snapshot)
            ids.forEach { manager.updateAppWidget(it, views) }

            val uri = snapshot.artworkUri ?: return
            scope.launch {
                val bitmap = loadArtwork(context, uri) ?: return@launch
                val withArt = buildViews(context, snapshot).apply {
                    setImageViewBitmap(R.id.widget_art, bitmap)
                }
                ids.forEach { manager.updateAppWidget(it, withArt) }
            }
        }

        private suspend fun loadArtwork(context: Context, uri: String): Bitmap? = runCatching {
            val request = ImageRequest.Builder(context)
                .data(uri)

                .allowHardware(false)
                .size(256)
                .build()
            ImageLoader(context).execute(request).image?.toBitmap()
        }.getOrNull()

        private fun buildViews(context: Context, snapshot: Snapshot): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_player).apply {
                setTextViewText(R.id.widget_title, snapshot.title)
                setTextViewText(R.id.widget_artist, snapshot.artist)
                setImageViewResource(
                    R.id.widget_play,
                    if (snapshot.isPlaying) R.drawable.ic_widget_pause
                    else R.drawable.ic_widget_play,
                )
                setOnClickPendingIntent(R.id.widget_prev, mediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
                setOnClickPendingIntent(R.id.widget_play, mediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                setOnClickPendingIntent(R.id.widget_next, mediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT))
                setOnClickPendingIntent(R.id.widget_root, openApp(context))
            }

        private fun openApp(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun mediaKey(context: Context, keyCode: Int): PendingIntent {
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                component = ComponentName(context, MusicService::class.java)
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            }
            return PendingIntent.getService(
                context,
                keyCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
