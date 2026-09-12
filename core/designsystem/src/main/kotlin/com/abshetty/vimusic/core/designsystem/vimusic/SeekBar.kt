package com.abshetty.vimusic.core.designsystem.vimusic

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SeekBar(
    value: Long,
    minimumValue: Long,
    maximumValue: Long,
    onDragStart: (Long) -> Unit,
    onDrag: (Long) -> Unit,
    onDragEnd: () -> Unit,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    barHeight: Dp = 3.dp,
    scrubberRadius: Dp = 6.dp,
    shape: Shape = RectangleShape,
) {
    var isDragging by remember { mutableStateOf(false) }
    val currentBarHeight by animateDpAsState(
        if (isDragging) scrubberRadius else barHeight,
        label = "barHeight",
    )
    val currentScrubberRadius by animateDpAsState(
        if (isDragging) 0.dp else scrubberRadius,
        label = "scrubber",
    )

    val range = (maximumValue - minimumValue).coerceAtLeast(1)
    val fraction = ((value - minimumValue).toFloat() / range).coerceIn(0f, 1f)

    BoxWithConstraints(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .padding(horizontal = scrubberRadius)
            .height(scrubberRadius * 2),
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)

        fun positionToValue(x: Float): Long =
            minimumValue + ((x / widthPx).coerceIn(0f, 1f) * range).toLong()

        Box(
            Modifier
                .fillMaxWidth()
                .height(currentBarHeight)
                .clip(shape)
                .background(backgroundColor)
        )
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(currentBarHeight)
                .clip(shape)
                .background(color)
        )

        Canvas(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .pointerInput(range) {
                    detectTapGestures { offset -> onDrag(positionToValue(offset.x)); onDragEnd() }
                }
                .pointerInput(range) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            onDragStart(positionToValue(offset.x))
                        },
                        onDragEnd = { isDragging = false; onDragEnd() },
                        onDragCancel = { isDragging = false; onDragEnd() },
                    ) { change, _ ->
                        onDrag(positionToValue(change.position.x))
                    }
                }
        ) {
            if (currentScrubberRadius > 0.dp) {
                drawCircle(
                    color = color,
                    radius = currentScrubberRadius.toPx(),
                    center = Offset(fraction * size.width, size.height / 2),
                )
            }
        }
    }
}
