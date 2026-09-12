package com.abshetty.vimusic.feature.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.abshetty.vimusic.core.data.repository.Lyrics

@Composable
fun LyricsView(
    lyrics: Lyrics?,
    positionMs: Long,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    when {
        loading -> Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

        lyrics == null || lyrics.isEmpty -> Box(modifier.fillMaxSize(), Alignment.Center) {
            Text(
                "No lyrics found for this track",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp),
            )
        }

        !lyrics.hasSynced -> Text(
            text = lyrics.plain.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        )

        else -> {
            val listState = rememberLazyListState()

            val activeIndex by remember(lyrics, positionMs) {
                derivedStateOf {
                    lyrics.synced.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
                }
            }

            LaunchedEffect(activeIndex) {
                runCatching {
                    listState.animateScrollToItem(
                        index = (activeIndex - 3).coerceAtLeast(0)
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
            ) {
                itemsIndexed(lyrics.synced) { index, line ->
                    val isActive = index == activeIndex
                    val colour by animateColorAsState(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "lyricColour",
                    )
                    Text(
                        text = line.text,
                        style = if (isActive) MaterialTheme.typography.headlineSmall
                        else MaterialTheme.typography.titleMedium,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = colour,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}
