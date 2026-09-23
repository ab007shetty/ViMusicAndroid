package com.abshetty.vimusic.feature.library

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abshetty.vimusic.core.designsystem.vimusic.Dimensions
import com.abshetty.vimusic.core.designsystem.vimusic.FloatingActionButton
import com.abshetty.vimusic.core.designsystem.vimusic.Header
import com.abshetty.vimusic.core.designsystem.vimusic.LocalAppearance
import com.abshetty.vimusic.core.designsystem.vimusic.NavigationRail
import com.abshetty.vimusic.core.designsystem.vimusic.PlaylistGridShimmer
import com.abshetty.vimusic.core.designsystem.vimusic.PlaylistItem
import com.abshetty.vimusic.core.designsystem.vimusic.RailIconButton
import com.abshetty.vimusic.core.designsystem.vimusic.SongItem
import com.abshetty.vimusic.core.designsystem.vimusic.SongListShimmer
import com.abshetty.vimusic.core.designsystem.vimusic.medium
import com.abshetty.vimusic.core.designsystem.vimusic.semiBold
import com.abshetty.vimusic.core.model.Playlist
import com.abshetty.vimusic.core.model.Song
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField

private val SORT_ICON_PADDING = 5.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenPlayer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val (colorPalette, typography) = LocalAppearance.current

    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Playlist?>(null) }
    var deleting by remember { mutableStateOf<Playlist?>(null) }
    var playlistMenu by remember { mutableStateOf<Playlist?>(null) }
    var songMenu by remember { mutableStateOf<Song?>(null) }
    var deletingSong by remember { mutableStateOf<Song?>(null) }
    var showSort by remember { mutableStateOf(false) }

    var filtering by remember { mutableStateOf(false) }

    LaunchedEffect(state.activeTab, state.activePlaylistId) { filtering = false }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) viewModel.setLocalFolder(uri) }

    val openPlaylist = state.playlists.firstOrNull { it.id == state.activePlaylistId }

    val snackbar = remember { SnackbarHostState() }
    val message by viewModel.message.collectAsStateWithLifecycle()
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    BackHandler(enabled = state.activePlaylistId != null) { viewModel.selectPlaylist(null) }

    LibraryOverlays(
        state = state,
        viewModel = viewModel,
        openPlaylist = openPlaylist,
        showCreate = showCreate, onCreateDismiss = { showCreate = false },
        renaming = renaming, onRenameDismiss = { renaming = null },
        deleting = deleting, onDeleteDismiss = { deleting = null },
        playlistMenu = playlistMenu,
        onPlaylistMenuDismiss = { playlistMenu = null },
        onRequestRename = { renaming = it },
        onRequestDelete = { deleting = it },
        songMenu = songMenu, onSongMenuDismiss = { songMenu = null },
        deletingSong = deletingSong,
        onDeleteSongDismiss = { deletingSong = null },
        onRequestDeleteSong = { deletingSong = it },
        showSort = showSort, onSortDismiss = { showSort = false },
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val tabs = state.tabs
    val tabIndex = tabs.indexOf(state.activeTab).coerceAtLeast(0)

    Row(modifier.fillMaxSize().background(colorPalette.background0)) {
        NavigationRail(
            topIcon = Icons.Rounded.Settings,
            onTopIconClick = onOpenSettings,
            tabIndex = tabIndex,
            onTabIndexChange = { viewModel.selectTab(tabs[it]) },
            isLandscape = isLandscape,

            showIcons = false,
            loading = state.isLoading,
            topSpace = Dimensions.headerHeight,
        ) { Item ->
            tabs.forEachIndexed { index, tab ->
                Item(index, tab.shortLabelFor(state.isGuest), tab.icon)
            }
        }

        AnimatedContent(
            targetState = state.activeTab,
            transitionSpec = {
                val direction =
                    if (tabs.indexOf(targetState) > tabs.indexOf(initialState)) {
                        AnimatedContentTransitionScope.SlideDirection.Up
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Down
                    }
                val animation = spring(
                    dampingRatio = 0.9f,
                    stiffness = Spring.StiffnessLow,
                    visibilityThreshold = IntOffset.VisibilityThreshold,
                )
                slideIntoContainer(direction, animation)
                    .togetherWith(slideOutOfContainer(direction, animation))
            },
            modifier = Modifier.weight(1f),
            label = "section",
        ) { _ ->
            Column(Modifier.fillMaxSize()) {
                Header(
                    title = openPlaylist?.name ?: state.title,
                    leading = if (openPlaylist != null) {
                        {
                            RailIconButton(
                                icon = Icons.Rounded.ArrowBack,
                                contentDescription = "Back to playlists",
                                tint = colorPalette.text,
                                onClick = { viewModel.selectPlaylist(null) },
                            )
                        }
                    } else null,
                    actions = {
                        val newPlaylist = state.activeTab == LibraryTab.PLAYLISTS &&
                            !state.isGuest &&
                            openPlaylist == null

                        if (newPlaylist) NewPlaylistBadge { showCreate = true }

                        RailIconButton(
                            Icons.Rounded.Search,
                            if (filtering) "Close filter" else "Filter this list",
                            size = 18.dp,
                            padding = SORT_ICON_PADDING,
                            tint = if (filtering || state.query.isNotBlank()) colorPalette.text
                            else colorPalette.textDisabled,
                        ) {
                            if (filtering) {
                                viewModel.setQuery("")
                                filtering = false
                            } else {
                                filtering = true
                            }
                        }

                        if (state.activeTab == LibraryTab.LOCAL) {
                            RailIconButton(
                                Icons.Rounded.FolderOpen,
                                if (state.hasLocalFolder) "Change folder" else "Choose folder",
                                size = 18.dp,
                                padding = SORT_ICON_PADDING,
                                tint = colorPalette.text,
                                onClick = { folderPicker.launch(null) },
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        SortKey.entries.forEach { key ->
                            val selected = key == state.sortKey
                            RailIconButton(
                                key.icon,
                                key.label,
                                size = 18.dp,
                                padding = SORT_ICON_PADDING,
                                tint = if (selected) colorPalette.text
                                else colorPalette.textDisabled,
                            ) {
                                if (selected) viewModel.setSort(key, !state.ascending)
                                else viewModel.setSort(key, key.defaultAscending)
                            }
                        }
                        RailIconButton(
                            if (state.ascending) Icons.Rounded.ArrowUpward
                            else Icons.Rounded.ArrowDownward,
                            if (state.ascending) "Ascending" else "Descending",
                            size = 18.dp,
                            padding = SORT_ICON_PADDING,
                            tint = colorPalette.text,
                        ) { viewModel.setSort(state.sortKey, !state.ascending) }
                    },
                )

                if (filtering) {
                    FilterField(
                        query = state.query,
                        placeholder = filterPlaceholderFor(state, openPlaylist?.name),
                        onQueryChange = viewModel::setQuery,
                        onClose = {
                            viewModel.setQuery("")
                            filtering = false
                        },
                    )
                }

                val showingPlaylistGrid =
                    state.activeTab == LibraryTab.PLAYLISTS && state.activePlaylistId == null

                val listState = rememberLazyListState()
                val gridState = rememberLazyGridState()

                LaunchedEffect(
                    state.sortKey,
                    state.ascending,
                    state.query,
                    state.activeTab,
                    state.activePlaylistId,
                ) {
                    listState.scrollToItem(0)
                    gridState.scrollToItem(0)
                }

                Box(Modifier.fillMaxSize()) {
                    when {
                        state.isLoading && showingPlaylistGrid -> PlaylistGridShimmer()
                        state.isLoading -> SongListShimmer()

                        showingPlaylistGrid -> PlaylistGrid(
                            playlists = state.playlists,
                            isGuest = state.isGuest,
                            gridState = gridState,
                            onOpen = viewModel::selectPlaylist,

                            onLongPress = { if (!state.isGuest) playlistMenu = it },
                        )

                        state.activeTab == LibraryTab.LOCAL && !state.hasLocalFolder ->
                            ChooseFolderPrompt { folderPicker.launch(null) }

                        else -> SongList(
                            songs = state.songs,
                            emptyText = emptyTextFor(state),
                            listState = listState,
                            onPlay = { viewModel.play(it); onOpenPlayer() },
                            onLongPress = { songMenu = it },
                        )
                    }

                    FloatingActionButton(
                        icon = Icons.Rounded.Search,
                        contentDescription = "Search",
                        onClick = onOpenSearch,
                        scrollState = if (showingPlaylistGrid) gridState else listState,
                    )

                    SnackbarHost(
                        hostState = snackbar,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    emptyText: String,
    listState: LazyListState,
    onPlay: (Song) -> Unit,
    onLongPress: (Song) -> Unit,
) {
    if (songs.isEmpty()) {
        EmptyState(emptyText)
        return
    }
    LazyColumn(
        state = listState,

        contentPadding = PaddingValues(bottom = 88.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(songs, key = { it.id }) { song ->
            SongItem(
                song = song,
                onClick = { onPlay(song) },
                onLongClick = { onLongPress(song) },
            )
        }
    }
}

@Composable
private fun PlaylistGrid(
    playlists: List<Playlist>,
    isGuest: Boolean,
    gridState: LazyGridState,
    onOpen: (Long) -> Unit,
    onLongPress: (Playlist) -> Unit,
) {
    if (playlists.isEmpty()) {
        EmptyState(
            if (isGuest) "No playlists yet."
            else "No playlists yet. Use the + button to make one."
        )
        return
    }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = Dimensions.thumbnails.playlist),
        contentPadding = PaddingValues(
            start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp,
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistItem(
                playlist = playlist,
                onClick = { onOpen(playlist.id) },
                onLongClick = { onLongPress(playlist) },
            )
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    val (colorPalette, typography) = LocalAppearance.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = typography.xs.semiBold,
            color = colorPalette.textDisabled,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp),
        )
    }
}

@Composable
private fun FilterField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    val (colorPalette, typography) = LocalAppearance.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colorPalette.background2)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(
            Icons.Rounded.Search,
            contentDescription = null,
            tint = colorPalette.textSecondary,
            modifier = Modifier.size(18.dp),
        )

        Box(
            Modifier
                .weight(1f)
                .padding(start = 10.dp, top = 10.dp, bottom = 10.dp),
        ) {
            if (query.isEmpty()) {
                Text(
                    placeholder,
                    style = typography.xs,
                    color = colorPalette.textDisabled,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = typography.xs.copy(color = colorPalette.text),
                cursorBrush = SolidColor(colorPalette.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        }

        RailIconButton(
            Icons.Rounded.Close,
            "Close filter",
            size = 18.dp,
            padding = SORT_ICON_PADDING,
            tint = colorPalette.textSecondary,
            onClick = onClose,
        )
    }
}

private fun filterPlaceholderFor(state: LibraryUiState, playlistName: String?): String {
    val where = when {
        playlistName != null -> playlistName
        state.activeTab == LibraryTab.PLAYLISTS -> "your playlists"
        else -> state.title.lowercase()
    }
    return "Filter " + where
}

private fun emptyTextFor(state: LibraryUiState): String = when {
    state.activeTab == LibraryTab.LOCAL ->
        "No audio files in " + (state.localFolderName ?: "that folder") +
            ". Use the folder button to pick another."
    !state.isOnline ->
        "You are offline. Connect to the internet, or play music from the Local tab."
    state.query.isNotBlank() -> "No songs match \"${state.query}\""
    else -> "Nothing here yet. Search for something to play."
}

@Composable
private fun ChooseFolderPrompt(onChoose: () -> Unit) {
    val (colorPalette, typography) = LocalAppearance.current
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Rounded.FolderOpen,
            contentDescription = null,
            tint = colorPalette.textDisabled,
            modifier = Modifier.size(48.dp),
        )
        Text(
            "Play music already on this phone",
            style = typography.s.semiBold,
            color = colorPalette.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Pick a folder and everything in it -- including its sub-folders -- " +
                "shows up here. Nothing is uploaded and nothing syncs.",
            style = typography.xxs,
            color = colorPalette.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            Modifier
                .padding(top = 24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colorPalette.accent)
                .clickable(onClick = onChoose)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Rounded.FolderOpen,
                contentDescription = null,
                tint = colorPalette.onAccent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "Choose folder",
                style = typography.xs.semiBold,
                color = colorPalette.onAccent,
            )
        }
    }
}

@Composable
private fun NewPlaylistBadge(onClick: () -> Unit) {
    val (colorPalette, typography) = LocalAppearance.current
    Row(

        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colorPalette.background2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Rounded.Add,
            contentDescription = null,
            tint = colorPalette.accent,
            modifier = Modifier.size(18.dp),
        )
        Text("New playlist", style = typography.xxs.semiBold, color = colorPalette.text)
    }
}
