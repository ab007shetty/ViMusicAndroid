package com.abshetty.vimusic.feature.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.abshetty.vimusic.core.designsystem.component.MorphingPlayButton
import com.abshetty.vimusic.core.designsystem.vimusic.Dimensions
import com.abshetty.vimusic.core.designsystem.vimusic.LocalAppearance
import com.abshetty.vimusic.core.designsystem.vimusic.ThumbnailRadius
import com.abshetty.vimusic.core.designsystem.vimusic.semiBold
import com.abshetty.vimusic.core.designsystem.rememberHaptics
import com.abshetty.vimusic.core.media.PlaybackUiState
import com.abshetty.vimusic.core.designsystem.R
import com.abshetty.vimusic.core.model.Artwork
import androidx.compose.ui.res.painterResource

@Composable
fun MiniPlayer(onExpand: () -> Unit, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.hasQueue) return

    MiniPlayerContent(
        state = state,
        onExpand = onExpand,
        onTogglePlay = viewModel::togglePlay,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onClose = viewModel::stop,
    )
}

@Composable
fun MiniPlayerContent(
    state: PlaybackUiState,
    onExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    var dragX by remember { mutableFloatStateOf(0f) }
    val offset by animateFloatAsState(dragX, label = "miniDrag")

    val progress = if (state.durationMs > 0) {
        (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
    } else 0f

    val (colorPalette, typography) = LocalAppearance.current

    Column(
        modifier = modifier
            .testTag("miniPlayer")
            .fillMaxWidth()

            .background(colorPalette.background1)
            .clickable(onClick = onExpand)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimensions.collapsedPlayer)
                .padding(horizontal = 12.dp)

                .graphicsLayer { translationX = offset }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when (GestureResolver.resolveSwipe(dragX, 0f, 0f, GestureThresholds())) {
                                GestureOutcome.Next -> { haptics.confirm(); onNext() }
                                GestureOutcome.Previous -> { haptics.confirm(); onPrevious() }
                                else -> haptics.reject()
                            }
                            dragX = 0f
                        },
                        onDragCancel = { dragX = 0f },
                    ) { _, amount -> dragX += amount }
                },
        ) {
            val noArtwork = painterResource(R.drawable.ic_default_artwork)
                .takeIf { Artwork.isOnDevice(state.artworkUri) }

            AsyncImage(
                model = state.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = noArtwork,
                fallback = noArtwork,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(ThumbnailRadius))
                    .background(colorPalette.background2),
            )

            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    state.title,
                    style = typography.xs.semiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.artist,
                    style = typography.xxs.semiBold,
                    color = colorPalette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            MorphingPlayButton(
                isPlaying = state.isPlaying,
                onClick = onTogglePlay,
                size = 44.dp,
            )

            IconButton(onClick = onClose) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Close player",
                    tint = colorPalette.textSecondary,
                )
            }
        }

        Box(Modifier.fillMaxWidth().height(2.dp)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = colorPalette.accent,
                trackColor = colorPalette.background2,
            )
        }
    }
}
