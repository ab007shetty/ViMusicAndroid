package com.abshetty.vimusic.core.media.volume

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.ComponentName
import android.media.AudioManager
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.abshetty.vimusic.core.datastore.SettingsStore
import com.abshetty.vimusic.core.media.MusicService
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VolumeKeyAccessibilityService : AccessibilityService() {
    @Inject lateinit var settings: SettingsStore

    private val haptics by lazy { SkipHaptics(this) }
    private val audioManager by lazy { getSystemService(AudioManager::class.java) }
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }
    private val keyguardManager by lazy { getSystemService(KeyguardManager::class.java) }
    private val displayManager by lazy { getSystemService(android.hardware.display.DisplayManager::class.java) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var settingsJob: Job? = null

    private var controller: MediaController? = null

    @Volatile
    private var detector = VolumeKeySkipDetector()

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo?.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }

        settingsJob?.cancel()
        settingsJob = scope.launch {
            settings.holdThresholdMs.collectLatest { detector = VolumeKeySkipDetector(it) }
        }

        connectController()
    }

    private fun connectController() {
        if (controller != null) return
        val token = SessionToken(this, ComponentName(this, MusicService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener(
            {
                controller = runCatching { future.get() }
                    .onFailure { Log.w(TAG, "no media session yet: " + it.message) }
                    .getOrNull()
                Log.i(TAG, "controller connected=" + (controller != null))
            },
            MoreExecutors.directExecutor(),
        )
    }

    private fun isEligible(): Boolean {
        val display = runCatching {
            displayManager?.getDisplay(android.view.Display.DEFAULT_DISPLAY)
        }.getOrNull()

        return display?.state != android.view.Display.STATE_ON ||
            powerManager?.isInteractive == false ||
            keyguardManager?.isKeyguardLocked == true
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val direction = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> SkipDirection.UP
            KeyEvent.KEYCODE_VOLUME_DOWN -> SkipDirection.DOWN
            else -> return false
        }

        val isRelease = event.action == KeyEvent.ACTION_UP
        if (event.action != KeyEvent.ACTION_DOWN && !isRelease) return false

        if (controller == null) connectController()

        val player = controller
        val eligible = isEligible()
        val playing = player?.isPlaying == true

        if (!eligible || !playing) {
            detector.reset()
            return false
        }

        val action = detector.onKey(
            direction = direction,
            downTime = event.downTime,
            eventTime = event.eventTime,
            isRelease = isRelease,
        )

        when (action) {
            is VolumeKeyAction.Skip -> skip(player, action.direction)
            is VolumeKeyAction.TapVolume -> adjustVolume(action.direction)
            VolumeKeyAction.Swallow -> Unit
        }

        if (isRelease) detector.reset()

        return true
    }

    private fun skip(player: MediaController, direction: SkipDirection) {
        Log.i(TAG, "skip " + direction)
        haptics.confirmSkip()
        if (direction == SkipDirection.UP) player.seekToNextMediaItem()
        else player.seekToPreviousMediaItem()
    }

    private fun adjustVolume(direction: SkipDirection) {
        val op = if (direction == SkipDirection.UP) AudioManager.ADJUST_RAISE
        else AudioManager.ADJUST_LOWER
        runCatching {
            audioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                op,
                AudioManager.FLAG_SHOW_UI,
            )
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = detector.reset()

    override fun onDestroy() {
        controller?.release()
        controller = null
        scope.cancel()
        super.onDestroy()
    }

    private companion object { const val TAG = "ViMusicVolume" }
}
