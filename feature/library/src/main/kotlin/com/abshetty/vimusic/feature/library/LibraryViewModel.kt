package com.abshetty.vimusic.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abshetty.vimusic.core.data.ConnectivityObserver
import com.abshetty.vimusic.core.data.auth.AuthRepository
import com.abshetty.vimusic.core.data.local.LocalMusicRepository
import com.abshetty.vimusic.core.data.repository.PlaylistRepository
import com.abshetty.vimusic.core.data.repository.SongRepository
import com.abshetty.vimusic.core.media.MediaIds
import com.abshetty.vimusic.core.media.MusicServiceConnection
import com.abshetty.vimusic.core.model.Playlist
import com.abshetty.vimusic.core.model.Song
import com.abshetty.vimusic.core.model.UserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import android.net.Uri
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val tabs: List<LibraryTab> = listOf(LibraryTab.MOST_PLAYED, LibraryTab.PLAYLISTS),
    val activeTab: LibraryTab = LibraryTab.FAVOURITES,
    val title: String = "Master's Mix",
    val songs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val activePlaylistId: Long? = null,
    val isOnline: Boolean = true,

    val isGuest: Boolean = false,

    val isLoading: Boolean = true,
    val query: String = "",
    val sortKey: SortKey = SortKey.ADDED_ON,
    val ascending: Boolean = false,

    val localFolderName: String? = null,
    val hasLocalFolder: Boolean = false,

    val canDeleteLocal: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val songs: SongRepository,
    private val playlists: PlaylistRepository,
    private val auth: AuthRepository,
    connectivity: ConnectivityObserver,
    private val player: MusicServiceConnection,
    private val local: LocalMusicRepository,
) : ViewModel() {
    init {
        viewModelScope.launch { local.refresh() }
    }

    private val activeTab = MutableStateFlow(LibraryTab.FAVOURITES)
    private val activePlaylistId = MutableStateFlow<Long?>(null)
    private val query = MutableStateFlow("")
    private val sortKey = MutableStateFlow(SortKey.ADDED_ON)
    private val ascending = MutableStateFlow(false)

    private val loadedLists = MutableStateFlow(emptySet<String>())

    private fun listKey(tab: LibraryTab, playlistId: Long?) = tab.name + ":" + playlistId

    init { correctTabForGuests() }

    private fun correctTabForGuests() {
        viewModelScope.launch {
            combine(auth.userId, auth.isResolved) { id, resolved -> id to resolved }
                .collect { (userId, resolved) ->
                    if (!resolved) return@collect
                    val allowed = LibraryTab.visibleFor(userId)
                    if (activeTab.value !in allowed) activeTab.value = allowed.first()
                }
        }
    }

    private val tabSongs =
        combine(activeTab, activePlaylistId, auth.userId) { tab, pid, uid -> Triple(tab, pid, uid) }
        .flatMapLatest { (tab, pid, userId) ->
            when (tab) {
                LibraryTab.SONGS -> songs.allSongs()
                LibraryTab.FAVOURITES -> songs.favourites()

                LibraryTab.MOST_PLAYED ->
                    if (UserId.isGuest(userId)) songs.favourites() else songs.mostPlayed()
                LibraryTab.RECENTLY_PLAYED -> songs.recentlyPlayed()

                LibraryTab.OFFLINE -> songs.offlinePlayable()
                LibraryTab.PLAYLISTS ->
                    if (pid == null) songs.favourites() else playlists.songsIn(pid)

                LibraryTab.LOCAL -> local.songs
            }.onEach { loadedLists.value = loadedLists.value + listKey(tab, pid) }
        }

    private data class Filters(val query: String, val key: SortKey, val ascending: Boolean)

    private val filters = combine(query, sortKey, ascending, ::Filters)

    private data class LocalState(
        val folderName: String?,
        val hasFolder: Boolean,
        val canDelete: Boolean,
        val scanning: Boolean,
    )

    private val localState =
        combine(local.folderName, local.folderUri, local.scanning) { name, uri, scanning ->
            LocalState(name, uri != null, local.canDelete(), scanning)
        }

    val uiState: StateFlow<LibraryUiState> = combine(
        auth.userId, activeTab, tabSongs, playlists.playlists(),
        connectivity.isOnline, filters, activePlaylistId, loadedLists, localState,
        auth.isResolved,
    ) { values ->
        val userId = values[0] as String
        val tab = values[1] as LibraryTab
        @Suppress("UNCHECKED_CAST") val list = values[2] as List<Song>
        @Suppress("UNCHECKED_CAST") val playlistList = values[3] as List<Playlist>
        val online = values[4] as Boolean
        val f = values[5] as Filters
        val pid = values[6] as Long?
        @Suppress("UNCHECKED_CAST") val loaded = values[7] as Set<String>
        val localInfo = values[8] as LocalState
        val authResolved = values[9] as Boolean

        val matching =
            LibraryFiltering.search(LibraryFiltering.forConnectivity(list, online), f.query)

        val visible = if (pid != null) {
            LibraryFiltering.sortInPlaylist(matching, f.key, f.ascending)
        } else {
            LibraryFiltering.sort(matching, f.key, f.ascending)
        }

        val guest = authResolved && UserId.isGuest(userId)

        LibraryUiState(
            tabs = LibraryTab.visibleFor(userId, authResolved),
            activeTab = tab,
            title = tab.labelFor(guest),
            songs = visible,

            playlists = if (pid == null && f.query.isNotBlank()) {
                playlistList.filter { it.name.contains(f.query.trim(), ignoreCase = true) }
            } else {
                playlistList
            },
            activePlaylistId = pid,
            isOnline = online,
            isGuest = guest,

            isLoading = !authResolved || listKey(tab, pid) !in loaded ||
                (tab == LibraryTab.LOCAL && localInfo.scanning && list.isEmpty()),
            query = f.query,
            sortKey = f.key,
            ascending = f.ascending,
            localFolderName = localInfo.folderName,
            hasLocalFolder = localInfo.hasFolder,
            canDeleteLocal = localInfo.canDelete,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun selectTab(tab: LibraryTab) {
        activeTab.value = tab
        if (tab != LibraryTab.PLAYLISTS) activePlaylistId.value = null
        query.value = ""
    }

    fun selectPlaylist(id: Long?) {
        activePlaylistId.value = id

        query.value = ""
    }

    fun setLocalFolder(uri: Uri) = viewModelScope.launch { local.setFolder(uri) }

    fun rescanLocalFolder() = viewModelScope.launch { local.refresh() }

    fun clearLocalFolder() = viewModelScope.launch { local.clearFolder() }

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun clearMessage() { _message.value = null }

    fun deleteLocalSong(song: Song) {
        viewModelScope.launch {
            if (!local.canDelete()) {
                _message.value = "This folder is read-only. Pick it again with the " +
                    "folder button to allow deleting."
                return@launch
            }

            player.removeFromQueue(song.id)
            _message.value = local.delete(song).fold(
                onSuccess = { "Deleted " + song.title },
                onFailure = { "Could not delete the file: " + (it.message ?: "unknown error") },
            )
        }
    }
    fun setQuery(value: String) { query.value = value }
    fun setSort(key: SortKey, asc: Boolean) { sortKey.value = key; ascending.value = asc }

    fun play(song: Song) {
        val list = uiState.value.songs
        val index = list.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        player.play(list.map(MediaIds::toMediaItem), index)
    }

    fun shuffleAll() {
        val list = uiState.value.songs.shuffled()
        if (list.isNotEmpty()) player.play(list.map(MediaIds::toMediaItem), 0)
    }

    fun addToQueue(song: Song) = player.addToQueue(MediaIds.toMediaItem(song))

    fun toggleFavourite(song: Song) {
        viewModelScope.launch { songs.toggleFavourite(song) }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch { playlists.create(name) }
    }

    fun renamePlaylist(id: Long, name: String) {
        viewModelScope.launch { playlists.rename(id, name) }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            playlists.delete(id)
            if (activePlaylistId.value == id) activePlaylistId.value = null
        }
    }

    fun setPlaylistCover(playlistId: Long, song: Song) {
        viewModelScope.launch { playlists.setCover(playlistId, song.thumbnailUrl) }
    }

    fun clearPlaylistCover(playlistId: Long) {
        viewModelScope.launch { playlists.setCover(playlistId, null) }
    }

    fun playlistIdsContaining(songId: String) = playlists.playlistIdsContaining(songId)

    fun togglePlaylistMembership(playlistId: Long, song: Song, isMember: Boolean) {
        viewModelScope.launch {
            if (isMember) playlists.removeSong(playlistId, song.id)
            else playlists.addSong(playlistId, song)
        }
    }

    fun removeFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch { playlists.removeSong(playlistId, songId) }
    }
}
