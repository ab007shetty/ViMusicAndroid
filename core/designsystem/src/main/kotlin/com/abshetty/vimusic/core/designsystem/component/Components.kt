package com.abshetty.vimusic.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.abshetty.vimusic.core.designsystem.rememberHaptics
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import com.abshetty.vimusic.core.designsystem.theme.Motion
import com.abshetty.vimusic.core.model.Song
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MorphingPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = rememberHaptics()

    val corner by animateDpAsState(
        targetValue = if (isPlaying) size / 2 else size / 4,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "playButtonCorner",
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(interactionSource = interaction, indication = null) {
                haptics.tap()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(size / 2.3f),
        )
    }
}

@Composable
fun MeshGradientScrim(seed: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mesh")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(24_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "meshPhase",
    )

    Canvas(modifier) {
        val blobs = listOf(
            Triple(0.30f, 0.25f, seed.copy(alpha = 0.55f)),
            Triple(0.75f, 0.40f, seed.copy(alpha = 0.35f)),
            Triple(0.50f, 0.80f, seed.copy(alpha = 0.28f)),
        )

        blobs.forEachIndexed { index, blob ->
            val drift = phase + index * 2.1f
            val center = Offset(
                x = size.width * (blob.first + 0.06f * cos(drift)),
                y = size.height * (blob.second + 0.06f * sin(drift)),
            )
            val radius = size.maxDimension * 0.55f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob.third, Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
    }
}

@Composable
fun SongCard(
    song: Song,
    onPlay: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()

    Column(
        modifier = modifier
            .testTag("songCard")
            .fillMaxWidth()
            .combinedClickable(
                onClick = { haptics.tap(); onPlay() },
                onLongClick = { haptics.confirm(); onLongPress() },
            )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            AsyncImage(
                model = com.abshetty.vimusic.core.model.Artwork.at(
                    song.thumbnailUrl, com.abshetty.vimusic.core.model.Artwork.THUMB
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )

            if (song.isPlayableOffline) {
                Icon(
                    Icons.Rounded.CloudDone,
                    contentDescription = "Available offline",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(50),
                        )
                        .padding(4.dp),
                )
            }

            if (song.isFavourite) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = "Favourite",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                )
            }
        }

        Text(
            text = song.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = song.artistsText ?: "Unknown Artist",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
