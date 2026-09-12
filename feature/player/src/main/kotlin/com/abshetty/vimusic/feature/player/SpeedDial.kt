package com.abshetty.vimusic.feature.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.abshetty.vimusic.core.designsystem.rememberHaptics
import com.abshetty.vimusic.core.designsystem.vimusic.LocalAppearance
import com.abshetty.vimusic.core.designsystem.vimusic.semiBold
import kotlin.math.roundToInt

@Composable
fun SpeedDial(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    range: ClosedFloatingPointRange<Float> = 0.5f..2.0f,
    label: String = "Speed",
) {
    val haptics = rememberHaptics()
    val (colorPalette, typography) = LocalAppearance.current

    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    var dragAnchor by remember { mutableFloatStateOf(value) }
    var accumulated by remember { mutableFloatStateOf(0f) }
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

    fun step(delta: Float) {
        val next = (value + delta).coerceIn(range.start, range.endInclusive)
        val stepped = (next * 20f).roundToInt() / 20f
        if (stepped != value) {
            haptics.segmentTick()
            onValueChange(stepped)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragAnchor = currentValue
                            accumulated = 0f
                        },
                        onDragEnd = { accumulated = 0f },
                        onDragCancel = { accumulated = 0f },
                    ) { change, drag ->

                        change.consume()
                        accumulated += drag
                        val span = range.endInclusive - range.start

                        val next = (dragAnchor + (accumulated / 260f) * span)
                            .coerceIn(range.start, range.endInclusive)
                        val stepped = (next * 20f).roundToInt() / 20f
                        if (stepped != currentValue) {
                            haptics.segmentTick()
                            currentOnValueChange(stepped)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        haptics.confirm()
                        onValueChange(1.0f)
                    })
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(size)) {
                val stroke = 10.dp.toPx()
                val inset = stroke / 2
                val sweep = 270f
                val start = 135f
                val arcSize = androidx.compose.ui.geometry.Size(
                    this.size.width - stroke, this.size.height - stroke,
                )

                drawArc(
                    color = colorPalette.background2,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    color = colorPalette.accent,
                    startAngle = start,
                    sweepAngle = sweep * fraction,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.2fx", value),
                    style = typography.l.semiBold,
                    color = colorPalette.text,
                )
                Text(
                    text = label,
                    style = typography.xxs,
                    color = colorPalette.textSecondary,
                )
            }
        }

        Row(
            Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton(Icons.Rounded.Remove, "Decrease $label") { step(-0.05f) }
            StepButton(Icons.Rounded.Add, "Increase $label") { step(0.05f) }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val (colorPalette) = LocalAppearance.current
    Box(
        Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = colorPalette.textSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}
