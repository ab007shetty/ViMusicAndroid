package com.abshetty.vimusic.core.designsystem.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object Motion {
    fun <T> spatialSpring() = spring<T>(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessMediumLow,
    )

    fun <T> effectsSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    fun <T> colorTransition() = tween<T>(durationMillis = 600)
}
