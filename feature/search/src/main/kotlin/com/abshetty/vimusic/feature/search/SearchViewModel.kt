package com.abshetty.vimusic.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abshetty.vimusic.core.data.ConnectivityObserver
import com.abshetty.vimusic.core.data.repository.SearchHistoryRepository
import com.abshetty.vimusic.core.data.auth.AuthRepository
import com.abshetty.vimusic.core.data.repository.PlaylistRepository
import com.abshetty.vimusic.core.data.repository.SongRepository
import com.abshetty.vimusic.core.innertube.SearchFilter
import com.abshetty.vimusic.core.innertube.SearchService
import com.abshetty.vimusic.core.innertube.model.SearchItem
import com.abshetty.vimusic.core.media.MediaIds
import com.abshetty.vimusic.core.media.MusicServiceConnection
import com.abshetty.vimusic.core.model.Song
import com.abshetty.vimusic.core.model.UserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val search: SearchService,
    private val player: MusicServiceConnection,
    private val songs: SongRepository,
    private val playlists: PlaylistRepository,
    auth: AuthRepository,
    private val history: SearchHistoryRepository,
    connectivity: ConnectivityObserver,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            history.recent(limit = 5).collect { entries ->
                _uiState.value = _uiState.value.copy(history = entries)
            }
        }

        viewModelScope.launch {
            auth.userId.collect { userId ->
                _uiState.value = _uiState.value.copy(isGuest = UserId.isGuest(userId))
            }
        }

        viewModelScope.launch {
            songs.favourites().collect { list ->
                _uiState.value = _uiState.value.copy(
                    favouriteIds = list.mapTo(mutableSetOf()) { it.id },
                )
            }
        }
    }

    fun forgetQuery(query: String) = viewModelScope.launch { history.remove(query) }

    fun playlists() = playlists.playlists()

    fun playlistIdsContaining(songId: String) = playlists.playlistIdsContaining(songId)

    fun toggleFavourite(song: Song) = viewModelScope.launch { songs.toggleFavourite(song) }

    fun togglePlaylistMembership(playlistId: Long, song: Song, isMember: Boolean) {
        viewModelScope.launch {
            if (isMember) playlists.removeSong(playlistId, song.id)
            else playlists.addSong(playlistId, song)
        }
    }

    fun setPlaylistCover(playlistId: Long, song: Song) {
        viewModelScope.launch { playlists.setCover(playlistId, song.thumbnailUrl) }
    }

    fun addToQueue(song: Song) = player.addToQueue(MediaIds.toMediaItem(song))

    init {
        viewModelScope.launch {
            _uiState.map { it.query }
                .debounce(250)
                .distinctUntilChanged()
                .filter { it.length >= 2 }
                .collect { q ->
                    search.suggestions(q).onSuccess { list ->
                        _uiState.value = _uiState.value.copy(suggestions = list.take(8))
                    }
                }
        }
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                _uiState.value = _uiState.value.copy(isOnline = online)
            }
        }
    }

    fun setQuery(value: String) { _uiState.value = _uiState.value.copy(query = value) }

    fun selectFilter(filter: SearchFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
        submit(_uiState.value.query)
    }

    fun submit(input: String) {
        when (val decision = SearchRouting.route(input)) {
            SearchRouting.Decision.Ignore -> Unit

            is SearchRouting.Decision.DirectVideo -> runSearch(decision.videoId, single = true)

            is SearchRouting.Decision.TextSearch -> runSearch(decision.query, single = false)
        }
    }

    private fun runSearch(query: String, single: Boolean) = viewModelScope.launch {
        if (!single) history.record(query)

        _uiState.value = _uiState.value.copy(
            isLoading = true, error = null, suggestions = emptyList()
        )
        search.search(query, _uiState.value.filter)
            .onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    items = if (single) result.items.take(1) else result.items,
                    continuation = if (single) null else result.continuation,
                    isLoading = false,
                )
            }
            .onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Search failed",
                )
            }
    }

    fun loadMore() {
        val state = _uiState.value
        val token = state.continuation ?: return
        if (state.isLoadingMore) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoadingMore = true)
            search.search(state.query, state.filter, token)
                .onSuccess { page ->
                    _uiState.value = _uiState.value.copy(
                        items = SearchPaging.merge(_uiState.value.items, page.items),
                        continuation = page.continuation,
                        isLoadingMore = false,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
        }
    }

    fun play(item: SearchItem) {
        val items = _uiState.value.items
        val index = items.indexOfFirst { it.videoId == item.videoId }.coerceAtLeast(0)
        player.play(items.map { MediaIds.toMediaItem(it.toSong()) }, index)

        viewModelScope.launch { songs.upsert(item.toSong()) }
    }

    fun clear() {
        _uiState.value = _uiState.value.copy(
            query = "", items = emptyList(), continuation = null,
            suggestions = emptyList(), error = null,
        )
    }
}

internal fun SearchItem.toSong() = Song(
    id = videoId, userId = "", title = title, artistsText = artistsText,
    durationText = durationText, thumbnailUrl = thumbnailUrl, channelId = channelId,
)
