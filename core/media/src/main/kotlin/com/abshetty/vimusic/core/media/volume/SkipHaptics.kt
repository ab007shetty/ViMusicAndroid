package com.abshetty.vimusic.core.media.volume

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class SkipHaptics(context: Context) {
    private val vibrator: Vibrator =
        context.getSystemService(VibratorManager::class.java).defaultVibrator

    fun confirmSkip() {
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)
        ) {
            vibrator.vibrate(
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.6f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f, 60)
                    .compose()
            )
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
