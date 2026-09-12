package com.abshetty.vimusic.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.abshetty.vimusic.core.model.LocalId
import com.abshetty.vimusic.core.model.Playlist
import com.abshetty.vimusic.core.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongActionsSheet(
    song: Song,
    playlists: List<Playlist>,
    containingPlaylistIds: Set<Long>,

    openPlaylist: Playlist?,

    isGuest: Boolean = false,
    onDismiss: () -> Unit,
    onToggleFavourite: () -> Unit,
    onAddToQueue: () -> Unit,
    onTogglePlaylist: (Playlist) -> Unit,
    onSetCover: (Playlist) -> Unit,

    onDelete: () -> Unit = {},
) {
    var choosingCover by remember { mutableStateOf(false) }

    val isLocal = LocalId.isLocal(song.id)
    val canSave = !isLocal && !isGuest
    val canSetCover = canSave && song.thumbnailUrl != null && playlists.isNotEmpty()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                song.artistsText ?: "Unknown Artist",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )

            if (isGuest && !isLocal) {
                Text(
                    "Sign in to save this song to your favourites or a playlist. " +
                        "Without an account there is nowhere to keep it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                )
                return@Column
            }

            if (choosingCover) {
                Text(
                    "Use this artwork for",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
                )
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(playlists, key = { it.id }) { playlist ->
                        SheetRow(icon = Icons.Rounded.Image, label = playlist.name) {
                            onSetCover(playlist); onDismiss()
                        }
                    }
                }
                return@Column
            }

            if (canSave) {
                SheetRow(
                    icon = Icons.Rounded.Favorite,
                    label = if (song.isFavourite) "Remove from favourites"
                    else "Add to favourites",
                ) { onToggleFavourite(); onDismiss() }
            }

            if (isLocal) {
                SheetRow(
                    icon = Icons.Rounded.DeleteOutline,
                    label = "Delete from device",
                    tint = MaterialTheme.colorScheme.error,
                ) { onDelete(); onDismiss() }
            }

            if (canSetCover) {
                if (openPlaylist != null) {
                    SheetRow(
                        icon = Icons.Rounded.Image,
                        label = "Use as cover for \"" + openPlaylist.name + "\"",
                    ) { onSetCover(openPlaylist); onDismiss() }
                } else {
                    SheetRow(icon = Icons.Rounded.Image, label = "Use as playlist cover") {
                        choosingCover = true
                    }
                }
            }

            if (canSave && playlists.isNotEmpty()) {
                Text(
                    "Playlists",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
                )
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(playlists, key = { it.id }) { playlist ->
                        val inIt = playlist.id in containingPlaylistIds
                        SheetRow(
                            icon = if (inIt) Icons.Rounded.Check else Icons.Rounded.PlaylistAdd,
                            label = playlist.name,
                            tint = if (inIt) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        ) { onTogglePlaylist(playlist) }
                    }
                }
            }
        }
    }
}

@Composable
fun SheetRow(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(label, color = tint, modifier = Modifier.padding(start = 16.dp))
    }
}
