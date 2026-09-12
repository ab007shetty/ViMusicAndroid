package com.abshetty.vimusic.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.abshetty.vimusic.core.designsystem.rememberHaptics
import com.abshetty.vimusic.core.model.Artwork
import com.abshetty.vimusic.core.model.Playlist
import com.abshetty.vimusic.core.model.Song
import kotlin.math.absoluteValue

fun accentFor(key: String): Pair<Color, Color> {
    val palette = listOf(
        Color(0xFF1DB954) to Color(0xFF0E7A38),
        Color(0xFF3D5AFE) to Color(0xFF1A237E),
        Color(0xFFFF6D00) to Color(0xFFBF360C),
        Color(0xFFD500F9) to Color(0xFF6A1B9A),
        Color(0xFF00BFA5) to Color(0xFF00695C),
        Color(0xFFFF1744) to Color(0xFF9B0022),
        Color(0xFF00B0FF) to Color(0xFF01579B),
    )
    return palette[(key.hashCode().absoluteValue) % palette.size]
}

@Composable
fun PlaylistTile(
    playlist: Playlist,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val (light, dark) = accentFor(playlist.name)

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(light, dark)))
            .combinedClickable(
                onClick = { haptics.tap(); onClick() },
                onLongClick = { haptics.confirm(); onLongPress() },
            )
    ) {
        val cover = playlist.coverUrl
        if (cover != null) {
            AsyncImage(
                model = Artwork.at(cover, Artwork.THUMB),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                        )
                    )
            )
        } else {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.align(Alignment.CenterEnd).size(96.dp).padding(end = 4.dp),
            )
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(
                playlist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                playlist.songCount.toString() + " songs",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
fun ColourfulSongCard(
    song: Song,
    onPlay: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val (light, dark) = accentFor(song.id)

    Box(
        modifier
            .testTag("songCard")
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))

            .background(Brush.linearGradient(listOf(light, dark)))
            .combinedClickable(
                onClick = { haptics.tap(); onPlay() },
                onLongClick = { haptics.confirm(); onLongPress() },
            )
    ) {
        AsyncImage(
            model = Artwork.at(song.thumbnailUrl, Artwork.THUMB),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(96.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                    )
                )
        )

        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(
                song.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artistsText ?: "Unknown Artist",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            if (song.isPlayableOffline) {
                Badge(Icons.Rounded.CloudDone, "Available offline", light)
            }
            if (song.isFavourite) {
                Badge(Icons.Rounded.Favorite, "Favourite", light)
            }
        }
    }
}

@Composable
private fun Badge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color,
) {
    Box(
        Modifier
            .padding(start = 4.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(5.dp)
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(14.dp))
    }
}
