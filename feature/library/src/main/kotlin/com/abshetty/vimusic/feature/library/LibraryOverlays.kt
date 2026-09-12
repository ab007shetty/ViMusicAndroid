package com.abshetty.vimusic.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abshetty.vimusic.core.designsystem.vimusic.ConfirmDialog
import com.abshetty.vimusic.core.model.Playlist
import com.abshetty.vimusic.core.model.Song
import com.abshetty.vimusic.core.designsystem.component.SongActionsSheet

@Composable
internal fun LibraryOverlays(
    state: LibraryUiState,
    viewModel: LibraryViewModel,
    openPlaylist: Playlist?,
    showCreate: Boolean,
    onCreateDismiss: () -> Unit,
    renaming: Playlist?,
    onRenameDismiss: () -> Unit,
    deleting: Playlist?,
    onDeleteDismiss: () -> Unit,
    playlistMenu: Playlist?,
    onPlaylistMenuDismiss: () -> Unit,
    onRequestRename: (Playlist) -> Unit,
    onRequestDelete: (Playlist) -> Unit,
    songMenu: Song?,
    onSongMenuDismiss: () -> Unit,
    deletingSong: Song?,
    onDeleteSongDismiss: () -> Unit,
    onRequestDeleteSong: (Song) -> Unit,
    showSort: Boolean,
    onSortDismiss: () -> Unit,
) {
    if (showCreate) {
        NamePlaylistDialog(
            title = "New playlist", confirmLabel = "Create",
            onDismiss = onCreateDismiss,
            onConfirm = viewModel::createPlaylist,
        )
    }

    renaming?.let { playlist ->
        NamePlaylistDialog(
            title = "Rename playlist", confirmLabel = "Save", initialName = playlist.name,
            onDismiss = onRenameDismiss,
            onConfirm = { viewModel.renamePlaylist(playlist.id, it) },
        )
    }

    deleting?.let { playlist ->
        DeletePlaylistDialog(
            playlist = playlist,
            onDismiss = onDeleteDismiss,
            onConfirm = { viewModel.deletePlaylist(playlist.id) },
        )
    }

    playlistMenu?.let { playlist ->
        PlaylistActionsSheet(
            playlist = playlist,
            onDismiss = onPlaylistMenuDismiss,
            onRename = { onRequestRename(playlist) },
            onDelete = { onRequestDelete(playlist) },
            onClearCover = { viewModel.clearPlaylistCover(playlist.id) },
        )
    }

    songMenu?.let { song ->
        val containing by viewModel.playlistIdsContaining(song.id)
            .collectAsStateWithLifecycle(initialValue = emptyList())
        SongActionsSheet(
            song = song,
            playlists = state.playlists,
            containingPlaylistIds = containing.toSet(),
            openPlaylist = openPlaylist,
            isGuest = state.isGuest,
            onDismiss = onSongMenuDismiss,
            onToggleFavourite = { viewModel.toggleFavourite(song) },
            onAddToQueue = { viewModel.addToQueue(song) },
            onTogglePlaylist = { p ->
                viewModel.togglePlaylistMembership(p.id, song, p.id in containing)
            },
            onSetCover = { p -> viewModel.setPlaylistCover(p.id, song) },
            onDelete = { onRequestDeleteSong(song) },
        )
    }

    deletingSong?.let { song ->
        ConfirmDialog(
            title = "Delete this file?",
            consequence = song.title +
                " will be removed from your device. This cannot be undone, and " +
                "it is the file itself, not just this list.",
            confirmLabel = "Delete",
            onDismiss = onDeleteSongDismiss,
            onConfirm = { viewModel.deleteLocalSong(song) },
        )
    }

    if (showSort) {
        FilterSheet(
            query = state.query,
            onQueryChange = viewModel::setQuery,
            onDismiss = onSortDismiss,
        )
    }
}
