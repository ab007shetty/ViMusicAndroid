package com.abshetty.vimusic.core.designsystem.vimusic

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.abshetty.vimusic.core.designsystem.R
import coil3.compose.AsyncImage
import com.abshetty.vimusic.core.designsystem.rememberHaptics
import com.abshetty.vimusic.core.model.Artwork
import com.abshetty.vimusic.core.model.Playlist
import com.abshetty.vimusic.core.model.Song
import kotlin.math.absoluteValue

@Composable
fun ItemContainer(
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(Dp) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.itemsVerticalPadding * 2),
        modifier = modifier
            .padding(
                vertical = Dimensions.itemsVerticalPadding,
                horizontal = 16.dp,
            )
            .fillMaxWidth(),
    ) {
        content(thumbnailSize)
    }
}

@Composable
fun SongItem(
    song: Song,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnailSize: Dp = Dimensions.thumbnails.song,
    trailing: @Composable (() -> Unit)? = null,
) {
    val (colorPalette, typography, thumbnailShape) = LocalAppearance.current
    val haptics = rememberHaptics()

    ItemContainer(
        thumbnailSize = thumbnailSize,
        modifier = modifier.combinedClickable(
            onClick = { haptics.tap(); onClick() },
            onLongClick = { haptics.confirm(); onLongClick() },
        ),
    ) { size ->
        Box(
            Modifier
                .size(size)
                .clip(thumbnailShape)
                .background(colorPalette.background2),
        ) {
            val onDevice = Artwork.isOnDevice(song.thumbnailUrl)
            val noArtwork = painterResource(R.drawable.ic_default_artwork).takeIf { onDevice }

            AsyncImage(
                model = song.thumbnailUrl?.let { Artwork.at(it, Artwork.THUMB) },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = noArtwork,
                fallback = noArtwork,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = song.title,
                style = typography.xs.semiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = song.artistsText ?: "Unknown artist",
                    style = typography.xs.semiBold,
                    color = colorPalette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                song.durationText?.let {
                    Text(
                        text = it,
                        style = typography.xxs.medium,
                        color = colorPalette.textSecondary,
                        maxLines = 1,
                    )
                }
            }
        }

        trailing?.invoke()
    }
}

@Composable
fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = Dimensions.thumbnails.playlist,
) {
    val (colorPalette, typography, thumbnailShape) = LocalAppearance.current
    val haptics = rememberHaptics()

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .combinedClickable(
                onClick = { haptics.tap(); onClick() },
                onLongClick = { haptics.confirm(); onLongClick() },
            )
            .padding(4.dp)
            .width(size),
    ) {
        Box(
            Modifier
                .size(size)
                .clip(thumbnailShape)
                .background(tintFor(playlist.name, colorPalette)),
            contentAlignment = Alignment.Center,
        ) {
            val cover = playlist.coverUrl
            if (cover != null) {
                AsyncImage(
                    model = Artwork.at(cover, Artwork.THUMB),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = playlist.name.take(2).uppercase(),
                    style = typography.l.semiBold,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
        Text(
            text = playlist.name,
            style = typography.xs.semiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (playlist.songCount == 1) "1 song" else "${playlist.songCount} songs",
            style = typography.xxs,
            color = colorPalette.textSecondary,
            maxLines = 1,
        )
    }
}

private fun tintFor(key: String, palette: ColorPalette): Brush {
    val hue = (key.hashCode().absoluteValue % 360).toFloat()
    val base = Color.hsl(hue, 0.35f, if (palette.isDark) 0.28f else 0.72f)
    val end = Color.hsl((hue + 24f) % 360f, 0.35f, if (palette.isDark) 0.18f else 0.62f)
    return Brush.linearGradient(listOf(base, end))
}

private val MinTitleSize = 20.sp

@Composable
fun Header(
    title: String,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,

    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    val (_, typography) = LocalAppearance.current

    Box(
        modifier
            .fillMaxWidth()
            .height(Dimensions.headerHeight)
            .padding(horizontal = 16.dp),
    ) {
        leading?.let {
            Box(Modifier.align(Alignment.CenterStart)) { it() }
        }

        var titleStyle by remember(title) { mutableStateOf(typography.xxl.medium) }
        Text(
            text = title,
            style = titleStyle,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.End,
            onTextLayout = { result ->
                if (result.didOverflowWidth && titleStyle.fontSize > MinTitleSize) {
                    titleStyle = titleStyle.copy(fontSize = titleStyle.fontSize * 0.92f)
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(start = 48.dp),
        )

        actions?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth(),
            ) { it() }
        }
    }
}

@Composable
fun RailIconButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = 20.dp,

    padding: Dp = 10.dp,
    onClick: () -> Unit,
) {
    val (colorPalette) = LocalAppearance.current
    Box(
        modifier
            .clip(androidx.compose.foundation.shape.CircleShape)
            .combinedClickable(onClick = onClick)
            .padding(padding),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint ?: colorPalette.textSecondary,
            modifier = Modifier.size(size),
        )
    }
}
