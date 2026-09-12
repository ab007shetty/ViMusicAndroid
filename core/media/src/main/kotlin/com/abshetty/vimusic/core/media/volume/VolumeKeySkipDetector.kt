package com.abshetty.vimusic.core.media.volume

enum class SkipDirection { UP, DOWN }

sealed interface VolumeKeyAction {
    data class Skip(val direction: SkipDirection) : VolumeKeyAction

    data class TapVolume(val direction: SkipDirection) : VolumeKeyAction

    data object Swallow : VolumeKeyAction
}

class VolumeKeySkipDetector(
    private val holdThresholdMs: Long = DEFAULT_HOLD_THRESHOLD_MS,
) {
    private var firedForDownTime = NO_PRESS

    fun onKey(
        direction: SkipDirection,
        downTime: Long,
        eventTime: Long,
        isRelease: Boolean,
    ): VolumeKeyAction {
        if (firedForDownTime == downTime) return VolumeKeyAction.Swallow

        if (eventTime - downTime >= holdThresholdMs) {
            firedForDownTime = downTime
            return VolumeKeyAction.Skip(direction)
        }

        return if (isRelease) VolumeKeyAction.TapVolume(direction)
        else VolumeKeyAction.Swallow
    }

    fun reset() {
        firedForDownTime = NO_PRESS
    }

    companion object {
        const val DEFAULT_HOLD_THRESHOLD_MS = 450L

        private const val NO_PRESS = -1L
    }
}
