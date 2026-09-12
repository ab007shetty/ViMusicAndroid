package com.abshetty.vimusic.feature.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

data class QueueEntry(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val isCurrent: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    entries: List<QueueEntry>,
    onDismiss: () -> Unit,
    onPlayIndex: (Int) -> Unit,
    onRemoveIndex: (Int) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Up next",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
                Text(
                    "Nothing queued.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@ModalBottomSheet
        }

        LazyColumn(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            itemsIndexed(entries, key = { i, e -> e.mediaId + "-" + i }) { index, entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPlayIndex(index) }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (entry.artworkUri != null) {
                        AsyncImage(
                            model = entry.artworkUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                        )
                    } else {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null)
                    }

                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            entry.title,
                            style = MaterialTheme.typography.titleSmall,

                            fontWeight = if (entry.isCurrent) FontWeight.Bold
                            else FontWeight.Normal,
                            color = if (entry.isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            entry.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    IconButton(onClick = { onRemoveIndex(index) }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove from queue")
                    }
                }
            }
        }
    }
}
