package com.abshetty.vimusic.feature.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import com.abshetty.vimusic.core.designsystem.vimusic.Dimensions
import com.abshetty.vimusic.core.model.LocalId
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.abshetty.vimusic.core.designsystem.chroma.ChromaScheme
import com.abshetty.vimusic.core.designsystem.chroma.ChromaState
import com.abshetty.vimusic.core.designsystem.component.MeshGradientScrim
import com.abshetty.vimusic.core.designsystem.component.MorphingPlayButton
import com.abshetty.vimusic.core.designsystem.vimusic.LocalAppearance
import com.abshetty.vimusic.core.designsystem.vimusic.SeekBar
import com.abshetty.vimusic.core.designsystem.vimusic.bold
import com.abshetty.vimusic.core.designsystem.vimusic.semiBold
import com.abshetty.vimusic.core.designsystem.rememberHaptics
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import com.abshetty.vimusic.core.designsystem.R
import com.abshetty.vimusic.core.model.Artwork
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.animateColorAsState

@Composable
fun FullPlayer(
    onOpenEqualizer: () -> Unit = {},

    expansion: Animatable<Float, AnimationVector1D>,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val seed = ChromaState.rememberSeedFor(state.artworkUri) ?: ChromaScheme.DEFAULT_SEED

    LaunchedEffect(state.isPlaying) {
        while (state.isPlaying) {
            delay(500)
            viewModel.refreshPosition()
        }
    }

    val video by viewModel.video.collectAsStateWithLifecycle()
    val embedded by viewModel.embeddedStatus.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    val isFavourite by viewModel.isFavourite.collectAsStateWithLifecycle()
    val isGuest by viewModel.isGuest.collectAsStateWithLifecycle()
    val (colorPalette, typography) = LocalAppearance.current
    val lyricsLoading by viewModel.lyricsLoading.collectAsStateWithLifecycle()

    var pane by remember { mutableStateOf(PlayerPane.ART) }
    var showQueue by remember { mutableStateOf(false) }

    val embeddedHost = remember(embedded?.videoId) { viewModel.embedded() }

    LaunchedEffect(state.mediaId) {
        viewModel.onTrackChanged()
        if (pane == PlayerPane.LYRICS) viewModel.loadLyrics()
    }

    if (showQueue) {
        QueueSheet(
            entries = viewModel.queueEntries(),
            onDismiss = { showQueue = false },
            onPlayIndex = viewModel::playQueueIndex,
            onRemoveIndex = viewModel::removeQueueIndex,
        )
    }

    var scrub by remember { mutableStateOf<Float?>(null) }

    var seekTargetMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(seekTargetMs, state.positionMs) {
        val target = seekTargetMs ?: return@LaunchedEffect
        if (kotlin.math.abs(state.positionMs - target) < SEEK_SETTLED_MS) {
            seekTargetMs = null
        } else {
            kotlinx.coroutines.delay(SEEK_GIVE_UP_MS)
            seekTargetMs = null
        }
    }

    val isLocalTrack = LocalId.isLocal(state.mediaId.orEmpty())

    val gestureScope = rememberCoroutineScope()
    val swipeX = remember { Animatable(0f) }
    val density = LocalDensity.current

    val travelPx = with(density) {
        (LocalConfiguration.current.screenHeightDp.dp - Dimensions.collapsedPlayer).toPx()
    }

    var artWidthPx by remember { mutableFloatStateOf(0f) }

    val shownPositionMs = seekTargetMs ?: state.positionMs
    val progress = scrub ?: if (state.durationMs > 0) {
        (shownPositionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
    } else 0f

    LaunchedEffect(pane, embeddedHost) {
        if (pane != PlayerPane.VIDEO) return@LaunchedEffect
        val host = embeddedHost ?: return@LaunchedEffect
        snapshotFlow { expansion.value }.collect { host.setOverlayAlpha(it) }
    }

    Box(
        modifier
            .testTag("fullPlayer")
            .fillMaxWidth()
            .graphicsLayer {
                val open = expansion.value.coerceIn(0f, 1f)

                val eased = FastOutSlowInEasing.transform(open)
                val shrink = COLLAPSED_SCALE + (1f - COLLAPSED_SCALE) * eased
                scaleX = shrink
                scaleY = shrink
                transformOrigin = TransformOrigin(0.5f, 1f)

                alpha = (eased / HANDOVER).coerceIn(0f, 1f)

                val radius = 20.dp.toPx() * (1f - eased)
                shape = RoundedCornerShape(topStart = radius, topEnd = radius)
                clip = true
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        gestureScope.launch {
                            val travelled = (1f - expansion.value) * travelPx
                            val collapse = GestureResolver.resolveSwipe(
                                0f, travelled, 0f, GestureThresholds(),
                            ) == GestureOutcome.Collapse
                            if (collapse) {
                                haptics.tap()

                                expansion.animateTo(0f, settleSpring())
                                onCollapse()
                            } else {
                                expansion.animateTo(1f, settleSpring())
                            }
                        }
                    },
                    onDragCancel = {
                        gestureScope.launch { expansion.animateTo(1f, settleSpring()) }
                    },
                ) { change, amount ->
                    change.consume()
                    gestureScope.launch {
                        val next = expansion.value - amount / travelPx
                        expansion.snapTo(
                            if (next > 1f) 1f + (next - 1f) / 8f else next.coerceAtLeast(0f)
                        )
                    }
                }
            }

            .background(MaterialTheme.colorScheme.background)
    ) {
        MeshGradientScrim(seed = seed, modifier = Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .pointerInput(travelPx) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            gestureScope.launch {
                                val travelled = (1f - expansion.value) * travelPx
                                val collapse = GestureResolver.resolveSwipe(
                                    0f, travelled, 0f, GestureThresholds(),
                                ) == GestureOutcome.Collapse
                                if (collapse) {
                                    haptics.tap()

                                    expansion.animateTo(0f, settleSpring())
                                    onCollapse()
                                } else {
                                    expansion.animateTo(1f, settleSpring())
                                }
                            }
                        },
                        onDragCancel = {
                            gestureScope.launch { expansion.animateTo(1f, settleSpring()) }
                        },
                    ) { change, amount ->
                        change.consume()
                        gestureScope.launch {
                            val delta = amount / travelPx
                            val next = expansion.value - delta
                            expansion.snapTo(
                                if (next > 1f) 1f + (next - 1f) / 8f else next.coerceAtLeast(0f)
                            )
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 12.dp)
                    .onSizeChanged { artWidthPx = it.width.toFloat() }
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(pane, state.mediaId, artWidthPx) {
                        if (pane == PlayerPane.LYRICS) return@pointerInput
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                gestureScope.launch {
                                    val width = artWidthPx.takeIf { it > 0f } ?: return@launch
                                    when (
                                        GestureResolver.resolveSwipe(
                                            swipeX.value, 0f, 0f, GestureThresholds(),
                                        )
                                    ) {
                                        GestureOutcome.Next -> {
                                            haptics.confirm()

                                            swipeX.animateTo(-width, tween(180))
                                            viewModel.next()
                                            swipeX.snapTo(0f)
                                        }
                                        GestureOutcome.Previous -> {
                                            haptics.confirm()
                                            swipeX.animateTo(width, tween(180))
                                            viewModel.previous()
                                            swipeX.snapTo(0f)
                                        }
                                        else -> swipeX.animateTo(0f, settleSpring())
                                    }
                                }
                            },
                            onDragCancel = {
                                gestureScope.launch { swipeX.animateTo(0f, settleSpring()) }
                            },
                        ) { change, amount ->
                            change.consume()
                            gestureScope.launch {
                                val next = swipeX.value + amount
                                val hasNeighbour = if (next > 0) {
                                    state.previousArtworkUri != null
                                } else {
                                    state.nextArtworkUri != null
                                }
                                swipeX.snapTo(if (hasNeighbour) next else next / 4f)
                            }
                        }
                    }
                    .pointerInput(pane) {
                        detectTapGestures(

                            onTap = {
                                if (pane == PlayerPane.VIDEO) return@detectTapGestures
                                haptics.tap()
                                if (pane == PlayerPane.LYRICS) {
                                    pane = PlayerPane.ART
                                } else {
                                    pane = PlayerPane.LYRICS
                                    viewModel.loadLyrics()
                                }
                            },
                            onDoubleTap = { offset ->
                                if (pane == PlayerPane.LYRICS) return@detectTapGestures
                                when (
                                    GestureResolver.resolveDoubleTap(offset.x, size.width.toFloat())
                                ) {
                                    SeekDirection.BACKWARD -> {
                                        haptics.confirm()
                                        viewModel.seekBy(-GestureResolver.SEEK_STEP_MS)
                                    }
                                    SeekDirection.FORWARD -> {
                                        haptics.confirm()
                                        viewModel.seekBy(GestureResolver.SEEK_STEP_MS)
                                    }
                                    SeekDirection.NONE -> Unit
                                }
                            },
                        )
                    }

                    .onGloballyPositioned { coordinates ->
                        if (pane == PlayerPane.VIDEO) return@onGloballyPositioned
                        val host = embeddedHost ?: return@onGloballyPositioned
                        val topLeft = coordinates.positionInWindow()
                        host.prepareAt(
                            x = topLeft.x.roundToInt(),
                            y = topLeft.y.roundToInt(),
                            width = coordinates.size.width,
                            height = coordinates.size.height,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                when (pane) {
                    PlayerPane.ART -> {
                        val width = artWidthPx.takeIf { it > 0f } ?: 1f
                        CarouselArt(state.previousArtworkUri, swipeX.value - width)
                        CarouselArt(state.artworkUri, swipeX.value)
                        CarouselArt(state.nextArtworkUri, swipeX.value + width)
                    }

                    PlayerPane.VIDEO -> {
                        val host = remember(embedded?.videoId) { viewModel.embedded() }
                        when {
                            host != null && embedded?.videoId == state.mediaId ->
                                EmbeddedVideoSurface(host)

                            embedded?.error != null -> UnavailableVideo(embedded?.error)

                            else -> UnavailableVideo(
                                "Video is not available for this track"
                            )
                        }
                    }

                    PlayerPane.LYRICS -> LyricsView(
                        lyrics = lyrics,
                        positionMs = state.positionMs,
                        loading = lyricsLoading,
                    )
                }
            }

            val metadataDrift = Modifier.graphicsLayer {
                val width = artWidthPx.takeIf { it > 0f } ?: 1f
                translationX = swipeX.value * 0.6f
                alpha = 1f - (kotlin.math.abs(swipeX.value) / width).coerceIn(0f, 1f)
            }

            Text(
                state.title,
                style = typography.l.bold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .then(metadataDrift)
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 1_200,
                        repeatDelayMillis = 1_200,
                    ),
            )
            Text(
                state.artist,
                style = typography.s.semiBold,
                color = colorPalette.textSecondary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .then(metadataDrift)
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 1_200,
                        repeatDelayMillis = 1_200,
                    ),
            )

            val duration = state.durationMs.coerceAtLeast(1)
            SeekBar(
                value = scrub?.let { (it * duration).toLong() } ?: shownPositionMs,
                minimumValue = 0,
                maximumValue = duration,
                onDragStart = { scrub = it.toFloat() / duration },
                onDrag = { scrub = it.toFloat() / duration },
                onDragEnd = {
                    scrub?.let {
                        val target = (it * state.durationMs).toLong()
                        seekTargetMs = target
                        viewModel.seekTo(target)
                    }
                    scrub = null
                },
                color = colorPalette.accent,
                backgroundColor = colorPalette.background2,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    formatTime(shownPositionMs),
                    style = typography.xxs.semiBold,
                    color = colorPalette.textSecondary,
                )
                Text(
                    formatTime(state.durationMs),
                    style = typography.xxs.semiBold,
                    color = colorPalette.textSecondary,
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isLocalTrack) {
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            contentDescription = "Equalizer",
                            tint = colorPalette.textSecondary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                } else if (!isGuest) {
                    IconButton(onClick = viewModel::toggleFavouriteCurrent) {
                        Icon(
                            Icons.Rounded.Favorite,
                            contentDescription = "Favourite",
                            tint = if (isFavourite) colorPalette.favouritesIcon
                            else colorPalette.textDisabled,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }

                if (!isLocalTrack) AudioVideoToggle(
                    videoActive = pane == PlayerPane.VIDEO,
                    onSelect = { wantVideo ->
                        if (wantVideo) {
                            pane = PlayerPane.VIDEO
                            if (!video.enabled) viewModel.toggleVideo()
                        } else {
                            pane = PlayerPane.ART
                            if (video.enabled) viewModel.toggleVideo()
                        }
                    },
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ModeToggle(
                    icon = Icons.Rounded.Shuffle,
                    label = "Shuffle",
                    active = state.shuffle,
                    onClick = viewModel::toggleShuffle,
                )
                IconButton(onClick = viewModel::previous) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = colorPalette.text,
                        modifier = Modifier.size(24.dp),
                    )
                }
                MorphingPlayButton(state.isPlaying, viewModel::togglePlay)
                IconButton(onClick = viewModel::next) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = colorPalette.text,
                        modifier = Modifier.size(24.dp),
                    )
                }
                ModeToggle(
                    icon = if (state.repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Rounded.RepeatOne
                    } else Icons.Rounded.Repeat,
                    label = "Repeat",
                    active = state.repeatMode != Player.REPEAT_MODE_OFF,
                    onClick = viewModel::cycleRepeat,
                )
            }
        }
    }
}

@Composable
private fun ModeToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val (colorPalette, _) = LocalAppearance.current
    val background by animateColorAsState(
        if (active) colorPalette.accent else Color.Transparent,
        label = "modeBackground",
    )
    val tint by animateColorAsState(
        if (active) colorPalette.onAccent else colorPalette.textDisabled,
        label = "modeTint",
    )

    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun AudioVideoToggle(
    videoActive: Boolean,
    onSelect: (Boolean) -> Unit,
) {
    val (colorPalette, typography) = LocalAppearance.current
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colorPalette.background2)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(false to "Audio", true to "Video").forEach { (isVideo, label) ->
            val active = isVideo == videoActive
            Row(
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) colorPalette.accent else Color.Transparent)
                    .clickable { onSelect(isVideo) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    if (isVideo) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = if (active) colorPalette.onAccent else colorPalette.textSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    label,
                    style = typography.xxs.semiBold,
                    color = if (active) colorPalette.onAccent else colorPalette.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun CarouselArt(artworkUri: String?, offsetX: Float) {
    if (artworkUri == null) return

    val noArtwork = painterResource(R.drawable.ic_default_artwork)
        .takeIf { Artwork.isOnDevice(artworkUri) }

    AsyncImage(
        model = artworkUri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        error = noArtwork,
        fallback = noArtwork,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationX = offsetX },
    )
}

private fun settleSpring() = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,

    visibilityThreshold = 0.001f,
)

private const val HANDOVER = 0.4f

private const val COLLAPSED_SCALE = 0.82f

private const val SEEK_SETTLED_MS = 1_500L

private const val SEEK_GIVE_UP_MS = 2_000L

@Composable
private fun EmbeddedVideoSurface(
    host: com.abshetty.vimusic.core.media.embedded.YouTubeIFrameHost,
) {
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                val topLeft = coordinates.positionInWindow()
                host.showOver(
                    x = topLeft.x.roundToInt(),
                    y = topLeft.y.roundToInt(),
                    width = coordinates.size.width,
                    height = coordinates.size.height,
                )
            }
    )
    DisposableEffect(host) {
        onDispose { host.hideAway() }
    }
}

@Composable
private fun UnavailableVideo(reason: String?) {
    val (colorPalette, typography) = LocalAppearance.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Rounded.Movie,
            contentDescription = null,
            tint = colorPalette.textDisabled,
            modifier = Modifier.size(36.dp),
        )
        Text(
            reason ?: "Video is not available for this track",
            style = typography.xs.semiBold,
            color = colorPalette.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
