package com.abshetty.vimusic.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import com.abshetty.vimusic.core.designsystem.vimusic.Dimensions
import com.abshetty.vimusic.core.designsystem.vimusic.LocalAppearance
import com.abshetty.vimusic.core.designsystem.vimusic.NavigationRail
import com.abshetty.vimusic.core.designsystem.vimusic.medium
import com.abshetty.vimusic.core.designsystem.vimusic.semiBold
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.abshetty.vimusic.core.innertube.SearchFilter
import com.abshetty.vimusic.core.model.Song
import com.abshetty.vimusic.core.designsystem.rememberHaptics
import com.abshetty.vimusic.core.designsystem.component.SongActionsSheet
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onOpenPlayer: () -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    initialQuery: String? = null,

    focusTrigger: Int = 0,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val focusRequester = remember { FocusRequester() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()

    var songMenu by remember { mutableStateOf<Song?>(null) }

    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(state.query, TextRange(state.query.length))
        )
    }

    LaunchedEffect(state.query) {
        if (field.text != state.query) {
            field = TextFieldValue(state.query, TextRange(state.query.length))
        }
    }
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank()) {
            viewModel.setQuery(initialQuery)
            viewModel.submit(initialQuery)
        }
    }

    LaunchedEffect(focusTrigger) {
        if (initialQuery.isNullOrBlank()) {
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.items.size - 5 && state.items.isNotEmpty()
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }

    val (colorPalette, typography) = LocalAppearance.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    val filters = remember { listOf(SearchFilter.SONGS, SearchFilter.VIDEOS) }

    songMenu?.let { song ->
        val playlists by viewModel.playlists()
            .collectAsStateWithLifecycle(initialValue = emptyList())
        val containing by viewModel.playlistIdsContaining(song.id)
            .collectAsStateWithLifecycle(initialValue = emptyList())
        val isFavourite = song.id in state.favouriteIds

        SongActionsSheet(
            song = song.copy(likedAt = if (isFavourite) song.likedAt ?: 1L else null),
            playlists = playlists,
            containingPlaylistIds = containing.toSet(),

            openPlaylist = null,
            isGuest = state.isGuest,
            onDismiss = { songMenu = null },
            onToggleFavourite = { viewModel.toggleFavourite(song) },
            onAddToQueue = { viewModel.addToQueue(song) },
            onTogglePlaylist = { playlist ->
                viewModel.togglePlaylistMembership(playlist.id, song, playlist.id in containing)
            },
            onSetCover = { playlist -> viewModel.setPlaylistCover(playlist.id, song) },
        )
    }

    Row(modifier.fillMaxSize().background(colorPalette.background0)) {
        NavigationRail(
            topIcon = Icons.Rounded.ArrowBack,
            onTopIconClick = onBack,
            tabIndex = filters.indexOf(state.filter).coerceAtLeast(0),
            onTabIndexChange = { viewModel.selectFilter(filters[it]) },
            isLandscape = isLandscape,

            showIcons = false,
            topSpace = Dimensions.headerHeight,
        ) { Item ->
            filters.forEachIndexed { index, filter ->
                Item(
                    index,
                    filter.label,
                    if (filter == SearchFilter.VIDEOS) Icons.Rounded.Movie
                    else Icons.Rounded.MusicNote,
                )
            }
        }

        Column(Modifier.weight(1f)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(Dimensions.headerHeight)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (field.text.isNotEmpty()) {
                IconButton(
                    onClick = {
                        viewModel.clear()
                        runCatching { focusRequester.requestFocus() }
                        keyboard?.show()
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = colorPalette.textSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                if (field.text.isEmpty()) {
                    Text(
                        "Enter a name",
                        style = typography.xxl.medium,
                        color = colorPalette.textDisabled,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = field,
                    onValueChange = {
                        field = it
                        viewModel.setQuery(it.text)
                    },
                    singleLine = true,
                    textStyle = typography.xxl.medium.copy(
                        color = colorPalette.text,
                        textAlign = TextAlign.End,
                    ),
                    cursorBrush = SolidColor(colorPalette.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboard?.hide()
                        viewModel.submit(field.text)
                    }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
        }

        if (!state.isOnline) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.CloudOff,
                    contentDescription = null,
                    tint = colorPalette.red,
                )
                Text(
                    "  Offline. Search needs a connection; downloaded songs are in Library.",
                    style = typography.xxs,
                    color = colorPalette.red,
                )
            }
        }

        state.error?.let { message ->
            Text(
                message,
                style = typography.xs.semiBold,
                color = colorPalette.red,
                modifier = Modifier.padding(16.dp),
            )
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.items.isEmpty() &&
                (state.query.isBlank() || state.suggestions.isNotEmpty()) -> {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.history, key = { "history:" + it }) { entry ->
                        QueryRow(
                            query = entry,
                            icon = Icons.Rounded.History,
                            onTap = {
                                viewModel.setQuery(entry)
                                viewModel.submit(entry)
                            },
                            onForget = { viewModel.forgetQuery(entry) },
                        )
                    }

                    items(state.suggestions, key = { "suggestion:" + it }) { entry ->
                        QueryRow(
                            query = entry,
                            icon = Icons.Rounded.Search,
                            onTap = {
                                viewModel.setQuery(entry)
                                viewModel.submit(entry)
                            },
                        )
                    }
                }
            }

            state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No results",
                    style = typography.xs.semiBold,
                    color = colorPalette.textDisabled,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(state.items, key = { it.videoId }) { item ->
                    Row(
                        Modifier
                            .testTag("searchResultRow")
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { viewModel.play(item); onOpenPlayer() },
                                onLongClick = {
                                    haptics.confirm()
                                    songMenu = item.toSong()
                                },
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = item.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)),
                        )
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                item.title,
                                style = typography.xs.semiBold,
                                color = colorPalette.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                listOfNotNull(item.artistsText, item.durationText)
                                    .joinToString(" · "),
                                style = typography.xxs.semiBold,
                                color = colorPalette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (state.isGuest) return@Row
                        val isFavourite = item.videoId in state.favouriteIds
                        IconButton(onClick = { viewModel.toggleFavourite(item.toSong()) }) {
                            Icon(
                                if (isFavourite) Icons.Rounded.Favorite
                                else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (isFavourite) {
                                    "Remove from favourites"
                                } else {
                                    "Add to favourites"
                                },
                                tint = if (isFavourite) colorPalette.favouritesIcon
                                else colorPalette.textDisabled,
                            )
                        }
                    }
                }
                if (state.isLoadingMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun QueryRow(
    query: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onTap: () -> Unit,
    onForget: (() -> Unit)? = null,
) {
    val (colorPalette, typography) = LocalAppearance.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = colorPalette.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            query,
            style = typography.xs.semiBold,
            color = colorPalette.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
        )
        if (onForget != null) {
            IconButton(onClick = onForget, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Remove \"" + query + "\" from history",
                    tint = colorPalette.textDisabled,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
