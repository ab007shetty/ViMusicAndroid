package com.abshetty.vimusic.feature.player

import kotlin.math.abs

data class GestureThresholds(
    val horizontalDistance: Float = 120f,
    val verticalDistance: Float = 120f,
    val velocity: Float = 800f,
)

enum class GestureOutcome { Settle, Next, Previous, Expand, Collapse }
enum class SeekDirection { BACKWARD, NONE, FORWARD }

object GestureResolver {
    const val SEEK_STEP_MS = 10_000L

    fun resolveSwipe(
        dragX: Float,
        dragY: Float,
        velocity: Float,
        thresholds: GestureThresholds,
    ): GestureOutcome {
        val horizontal = abs(dragX) > abs(dragY)

        val fastEnough = abs(velocity) >= thresholds.velocity
        val farEnough = if (horizontal) {
            abs(dragX) >= thresholds.horizontalDistance
        } else {
            abs(dragY) >= thresholds.verticalDistance
        }

        if (!farEnough && !fastEnough) return GestureOutcome.Settle

        return when {
            horizontal && dragX < 0 -> GestureOutcome.Next
            horizontal -> GestureOutcome.Previous
            dragY > 0 -> GestureOutcome.Collapse
            else -> GestureOutcome.Expand
        }
    }

    fun resolveDoubleTap(tapX: Float, width: Float): SeekDirection = when {
        tapX < width / 3f -> SeekDirection.BACKWARD
        tapX > width * 2f / 3f -> SeekDirection.FORWARD
        else -> SeekDirection.NONE
    }
}
