package com.abshetty.vimusic.core.designsystem.vimusic

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.shimmer(): Modifier {
    val (colorPalette) = LocalAppearance.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    val start = progress * 2000f - 600f
    return this.background(
        Brush.linearGradient(
            colors = listOf(
                colorPalette.background2,
                colorPalette.background2.copy(alpha = 0.45f),
                colorPalette.background2,
            ),
            start = Offset(start, 0f),
            end = Offset(start + 600f, 0f),
        )
    )
}

@Composable
private fun ShimmerBlock(width: Dp?, height: Dp, corner: Dp = 6.dp) {
    Box(
        Modifier
            .then(if (width == null) Modifier.fillMaxWidth() else Modifier.size(width, height))
            .height(height)
            .clip(RoundedCornerShape(corner))
            .shimmer()
    )
}

@Composable
fun SongListShimmer(rows: Int = 8, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        repeat(rows) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = Dimensions.itemsVerticalPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    Modifier
                        .size(Dimensions.thumbnails.song)
                        .clip(RoundedCornerShape(ThumbnailRadius))
                        .shimmer()
                )
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ShimmerBlock(width = if (it % 2 == 0) 200.dp else 150.dp, height = 13.dp)
                    ShimmerBlock(width = if (it % 3 == 0) 110.dp else 90.dp, height = 11.dp)
                }
            }
        }
    }
}

@Composable
fun PlaylistGridShimmer(tiles: Int = 6, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat((tiles + 1) / 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            Modifier
                                .size(Dimensions.thumbnails.playlist)
                                .clip(RoundedCornerShape(ThumbnailRadius))
                                .shimmer()
                        )
                        ShimmerBlock(width = 96.dp, height = 12.dp)
                        ShimmerBlock(width = 60.dp, height = 10.dp)
                    }
                }
            }
        }
    }
}
