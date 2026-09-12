package com.abshetty.vimusic.core.designsystem.vimusic

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.abshetty.vimusic.core.designsystem.rememberHaptics
import kotlin.math.abs

@Composable
fun HapticSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true,
    stepSize: Float = ((valueRange.endInclusive - valueRange.start) / 20f).coerceAtLeast(1f),
    colors: SliderColors? = null,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val haptics = rememberHaptics()
    val palette = LocalAppearance.current.colorPalette

    var lastTickAt by remember { mutableFloatStateOf(value) }

    Slider(
        value = value,
        onValueChange = { next ->
            if (abs(next - lastTickAt) >= stepSize) {
                lastTickAt = next
                haptics.segmentTick()
            }
            onValueChange(next)
        },
        onValueChangeFinished = {
            lastTickAt = value
            onValueChangeFinished?.invoke()
        },
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        colors = colors ?: SliderDefaults.colors(
            thumbColor = palette.accent,
            activeTrackColor = palette.accent,
            inactiveTrackColor = palette.background2,
            disabledThumbColor = palette.textDisabled,
            disabledActiveTrackColor = palette.textDisabled,
            disabledInactiveTrackColor = palette.background2,
        ),
        modifier = modifier,
    )
}
