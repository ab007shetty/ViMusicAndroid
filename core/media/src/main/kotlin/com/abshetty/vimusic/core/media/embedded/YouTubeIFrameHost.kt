package com.abshetty.vimusic.core.media.embedded

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayInputStream

enum class EmbeddedState { IDLE, BUFFERING, PLAYING, PAUSED, ENDED }

data class EmbeddedStatus(
    val state: EmbeddedState = EmbeddedState.IDLE,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val videoId: String? = null,

    val error: String? = null,
    val ready: Boolean = false,
)

@SuppressLint("SetJavaScriptEnabled")
class YouTubeIFrameHost(context: Context) {
    private val _status = MutableStateFlow(EmbeddedStatus())
    val status: StateFlow<EmbeddedStatus> = _status.asStateFlow()

    private var windowAttached = false
    private var pageLoaded = false

    @Volatile private var seekTargetMs: Long? = null
    @Volatile private var seekDeadlineMs: Long = 0

    private val pending = mutableListOf<() -> Unit>()

    private val container: android.widget.FrameLayout =
        object : android.widget.FrameLayout(context) {}.apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerPx)
                }
            }
            clipToOutline = true
        }

    private val cornerPx: Float = 28f * context.resources.displayMetrics.density

    private var beforeFullscreen: android.graphics.Rect? = null

    private var lastShown: android.graphics.Rect? = null
    private var foregroundWatched = false
    private var customView: android.view.View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    val webView: WebView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true

        settings.mediaPlaybackRequiresUserGesture = false
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true

        settings.userAgentString = CHROME_UA
        addJavascriptInterface(Bridge(), BRIDGE)

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                if (m.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                    Log.i(TAG, "console: " + m.message())
                }
                return true
            }

            override fun onShowCustomView(
                view: android.view.View?,
                callback: CustomViewCallback?,
            ) {
                if (view == null) return
                customView = view
                customViewCallback = callback
                container.addView(
                    view,
                    android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
                enterFullscreen()
            }

            override fun onHideCustomView() {
                customView?.let { container.removeView(it) }
                customView = null
                runCatching { customViewCallback?.onCustomViewHidden() }
                customViewCallback = null
                exitFullscreen()
            }
        }

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?,
            ): WebResourceResponse? {
                if (request?.url?.host != HOST) return null
                return WebResourceResponse(
                    "text/html",
                    "utf-8",
                    ByteArrayInputStream(PAGE.toByteArray(Charsets.UTF_8)),
                )
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                Log.i(TAG, "page finished: " + url)
                pageLoaded = true
                val queued = pending.toList()
                pending.clear()
                queued.forEach { it() }
            }
        }
    }

    private inner class Bridge {
        @JavascriptInterface
        fun log(message: String) { Log.i(TAG, "js: " + message) }

        @JavascriptInterface
        fun onReady() {
            _status.value = _status.value.copy(ready = true)
        }

        @JavascriptInterface
        fun onState(code: Int) {
            val state = when (code) {
                -1 -> EmbeddedState.IDLE
                0 -> EmbeddedState.ENDED
                1 -> EmbeddedState.PLAYING
                2 -> EmbeddedState.PAUSED
                3 -> EmbeddedState.BUFFERING
                else -> _status.value.state
            }
            _status.value = _status.value.copy(state = state, error = null)
        }

        @JavascriptInterface
        fun onProgress(positionSeconds: Double, durationSeconds: Double) {
            val position = (positionSeconds * 1000).toLong().coerceAtLeast(0)
            val duration = (durationSeconds * 1000).toLong().coerceAtLeast(0)

            val target = seekTargetMs
            if (target != null) {
                val arrived = kotlin.math.abs(position - target) < SEEK_TOLERANCE_MS
                val gaveUp = System.currentTimeMillis() > seekDeadlineMs
                if (arrived || gaveUp) {
                    seekTargetMs = null
                } else {
                    if (duration > 0) {
                        _status.value = _status.value.copy(durationMs = duration)
                    }
                    return
                }
            }

            _status.value = _status.value.copy(positionMs = position, durationMs = duration)
        }

        @JavascriptInterface
        fun onError(code: Int) {
            val message = when (code) {
                2 -> "YouTube rejected this video id"
                5 -> "This video cannot be played in an embedded player"
                100 -> "This video has been removed or made private"
                101, 150 -> "The uploader does not allow this video to be embedded"
                else -> "Playback failed (code " + code + ")"
            }
            Log.w(TAG, "iframe error " + code + ": " + message)
            _status.value = _status.value.copy(error = message, state = EmbeddedState.IDLE)
        }
    }

    fun load() {
        webView.loadUrl(PAGE_URL)
    }

    private fun watchForegroundState(context: Context) {
        if (foregroundWatched) return
        val app = context.applicationContext as? android.app.Application ?: return
        foregroundWatched = true
        app.registerActivityLifecycleCallbacks(
            object : android.app.Application.ActivityLifecycleCallbacks {
                private var started = 0

                override fun onActivityStarted(activity: android.app.Activity) {
                    started++

                    lastShown?.let { showOver(it.left, it.top, it.width(), it.height()) }
                }

                override fun onActivityStopped(activity: android.app.Activity) {
                    started--
                    if (started <= 0) retreat()
                }

                override fun onActivityCreated(
                    activity: android.app.Activity,
                    state: android.os.Bundle?,
                ) = Unit
                override fun onActivityResumed(activity: android.app.Activity) = Unit
                override fun onActivityPaused(activity: android.app.Activity) = Unit
                override fun onActivitySaveInstanceState(
                    activity: android.app.Activity,
                    state: android.os.Bundle,
                ) = Unit
                override fun onActivityDestroyed(activity: android.app.Activity) = Unit
            }
        )
    }

    private fun call(js: String) {
        val run = { webView.evaluateJavascript(js, null) }
        if (pageLoaded) webView.post(run) else pending += run
    }

    fun play(videoId: String, startMs: Long = 0) {
        Log.i(TAG, "load " + videoId + " at " + startMs + "ms")
        seekTargetMs = null
        _status.value = _status.value.copy(
            videoId = videoId,
            error = null,
            positionMs = startMs,

            state = EmbeddedState.BUFFERING,

            durationMs = 0,
        )
        call("load('" + videoId.escaped() + "', " + (startMs / 1000.0) + ");")
    }

    fun resume() = call("resume();")
    fun pause() = call("pauseIt();")
    fun seekTo(positionMs: Long) {
        seekTargetMs = positionMs
        seekDeadlineMs = System.currentTimeMillis() + SEEK_GRACE_MS

        _status.value = _status.value.copy(positionMs = positionMs)
        call("seek(" + (positionMs / 1000.0) + ");")
    }
    fun setVolume(volume: Float) = call("setVol(" + (volume * 100).toInt().coerceIn(0, 100) + ");")
    fun stop() = call("stopIt();")

    fun attachToOwnWindow(context: Context): Boolean {
        if (!android.provider.Settings.canDrawOverlays(context)) {
            Log.i(TAG, "no overlay permission; the UI has to host the player")
            return false
        }
        if (windowAttached) return true

        return runCatching {
            detachFromParent()
            val params = android.view.WindowManager.LayoutParams(
                PLAYER_PX, PLAYER_PX,
                android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or

                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.graphics.PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.START

                windowAnimations = 0

                alpha = 0f
            }

            container.addView(
                webView,
                android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
            context.getSystemService(android.view.WindowManager::class.java)
                .addView(container, params)
            windowAttached = true
            watchForegroundState(context)
            true
        }.onFailure { Log.w(TAG, "could not attach overlay: " + it.message) }
            .getOrDefault(false)
    }

    private fun removeOwnWindow(context: Context) {
        if (!windowAttached) return
        runCatching {
            context.getSystemService(android.view.WindowManager::class.java).removeView(container)
        }
        windowAttached = false
    }

    fun detachFromParent() {
        (webView.parent as? ViewGroup)?.removeView(webView)
    }

    fun setOverlayAlpha(alpha: Float) {
        if (!windowAttached || lastShown == null) return
        val params = container.layoutParams as? android.view.WindowManager.LayoutParams ?: return
        val wanted = alpha.coerceIn(0f, 1f)
        if (kotlin.math.abs(params.alpha - wanted) < 0.02f) return
        updateWindow { it.alpha = wanted }
    }

    fun prepareAt(x: Int, y: Int, width: Int, height: Int) {
        if (!windowAttached || width <= 0 || height <= 0) return
        val params = container.layoutParams as? android.view.WindowManager.LayoutParams ?: return

        if (params.alpha != 0f) return
        if (params.x == x && params.y == y &&
            params.width == width && params.height == height
        ) return

        updateWindow {
            it.x = x
            it.y = y
            it.width = width
            it.height = height
        }
    }

    fun showOver(x: Int, y: Int, width: Int, height: Int) {
        if (!windowAttached || width <= 0 || height <= 0) return
        lastShown = android.graphics.Rect(x, y, x + width, y + height)

        val params = container.layoutParams as? android.view.WindowManager.LayoutParams
        val moved = params == null ||
            params.x != x || params.y != y ||
            params.width != width || params.height != height
        val hidden = params?.alpha == 0f

        if (moved) {
            updateWindow {
                it.x = x
                it.y = y
                it.width = width
                it.height = height
                if (!hidden) it.alpha = 1f
            }
        }

        val reveal = {
            updateWindow {
                it.alpha = 1f

                it.flags = it.flags and
                    android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            }
        }

        if (moved && hidden) container.post(reveal) else reveal()
    }

    fun hideAway() {
        lastShown = null
        retreat()
    }

    private fun retreat() {
        if (!windowAttached) return
        beforeFullscreen = null
        updateWindow {
            it.alpha = 0f
            it.flags = it.flags or
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
    }

    private fun updateWindow(change: (android.view.WindowManager.LayoutParams) -> Unit) {
        val params = container.layoutParams as? android.view.WindowManager.LayoutParams ?: return
        change(params)
        runCatching {
            container.context.getSystemService(android.view.WindowManager::class.java)
                .updateViewLayout(container, params)
        }.onFailure { Log.w(TAG, "could not move the player window: " + it.message) }
    }

    private fun enterFullscreen() {
        val params = container.layoutParams as? android.view.WindowManager.LayoutParams ?: return
        beforeFullscreen = android.graphics.Rect(
            params.x, params.y, params.x + params.width, params.y + params.height,
        )
        container.clipToOutline = false
        updateWindow {
            it.x = 0
            it.y = 0
            it.width = android.view.WindowManager.LayoutParams.MATCH_PARENT
            it.height = android.view.WindowManager.LayoutParams.MATCH_PARENT
        }
    }

    private fun exitFullscreen() {
        val previous = beforeFullscreen ?: return
        beforeFullscreen = null
        container.clipToOutline = true
        showOver(previous.left, previous.top, previous.width(), previous.height())
    }

    fun release() {
        runCatching { removeOwnWindow(container.context) }
        detachFromParent()
        runCatching { webView.destroy() }
    }

    private fun String.escaped() = replace("\\", "\\\\").replace("'", "\\'")

    private companion object {
        const val TAG = "ViMusicEmbedded"
        const val BRIDGE = "AndroidBridge"

        const val HOST = "player.vimusic.invalid"
        const val PAGE_URL = "https://$HOST/player.html"

        const val PLAYER_PX = 480

        const val SEEK_TOLERANCE_MS = 2_000L

        const val SEEK_GRACE_MS = 5_000L

        const val CHROME_UA = "Mozilla/5.0 (Linux; Android 14; SM-A536E) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

        val PAGE = """
            <!doctype html><html><head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
              html,body{margin:0;padding:0;background:#000;height:100%;overflow:hidden}
              #player{width:100%;height:100%}
            </style></head>
            <body><div id="player"></div>
            <script>
              var player = null, apiReady = false, playerReady = false, queued = null;
              var wantId = null;
              var wantPlay = true;

              function onYouTubeIframeAPIReady() {
                $BRIDGE.log('api ready');
                apiReady = true;
                if (queued) { var q = queued; queued = null; create(q.id, q.start); }
              }

              function create(id, start) {
                $BRIDGE.log('creating player for ' + id);
                wantId = id;
                player = new YT.Player('player', {
                  height: '100%', width: '100%',
                  videoId: id,
                  playerVars: {
                    autoplay: 1, controls: 1, enablejsapi: 1, rel: 0,
                    modestbranding: 1, fs: 1, playsinline: 1,
                    iv_load_policy: 3, cc_load_policy: 0,
                    start: Math.floor(start || 0),
                    origin: window.location.origin
                  },
                  events: {
                    onReady: function () {
                      playerReady = true;
                      $BRIDGE.onReady();
                      if (wantPlay) player.playVideo(); else player.pauseVideo();
                    },
                    onStateChange: function (e) {
                      if (e.data === 0) { try { player.stopVideo(); } catch (x) {} }
                      $BRIDGE.onState(e.data);
                    },
                    onError: function (e) { $BRIDGE.onError(e.data); }
                  }
                });
              }

              function load(id, start) {
                wantId = id;
                if (!apiReady) { queued = {id: id, start: start}; return; }
                if (!player) { create(id, start); return; }
                player.loadVideoById({videoId: id, startSeconds: start});
              }
              function resume() { wantPlay = true; if (playerReady) player.playVideo(); }
              function pauseIt() { wantPlay = false; if (playerReady) player.pauseVideo(); }
              function stopIt() { if (playerReady) player.stopVideo(); }
              function seek(s) { if (playerReady) player.seekTo(s, true); }
              function setVol(v) { if (playerReady) player.setVolume(v); }

              setInterval(function () {
                if (!playerReady || !player.getCurrentTime) return;
                try {
                  var data = player.getVideoData ? player.getVideoData() : null;
                  if (wantId && data && data.video_id && data.video_id !== wantId) return;
                  var d = player.getDuration();
                  if (!d) return;
                  $BRIDGE.onProgress(player.getCurrentTime(), d);
                } catch (e) {}
              }, 250);
            </script>
            <script src="https://www.youtube.com/iframe_api"></script>
            </body></html>
        """.trimIndent()
    }
}
