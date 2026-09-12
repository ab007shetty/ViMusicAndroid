package com.abshetty.vimusic.core.designsystem

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

class Haptics(private val view: android.view.View) {
    private val vibrator: Vibrator? = runCatching {
        val context = view.context
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }.getOrNull()

    fun tap() = feedback(HapticFeedbackConstants.CONTEXT_CLICK, durationMs = 10, amplitude = 60)

    fun confirm() = feedback(HapticFeedbackConstants.CONFIRM, durationMs = 20, amplitude = 120)

    fun reject() = feedback(HapticFeedbackConstants.REJECT, durationMs = 30, amplitude = 150)

    fun segmentTick() =
        feedback(HapticFeedbackConstants.SEGMENT_TICK, durationMs = 8, amplitude = 45)

    private fun feedback(constant: Int, durationMs: Long, amplitude: Int) {
        val handled = runCatching { view.performHapticFeedback(constant) }.getOrDefault(false)
        if (handled) return

        runCatching {
            vibrator?.takeIf { it.hasVibrator() }?.vibrate(
                VibrationEffect.createOneShot(durationMs, amplitude)
            )
        }
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}
