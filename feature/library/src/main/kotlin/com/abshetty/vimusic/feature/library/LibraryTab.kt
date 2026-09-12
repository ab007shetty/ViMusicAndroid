package com.abshetty.vimusic.feature.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.ui.graphics.vector.ImageVector
import com.abshetty.vimusic.core.model.Song
import com.abshetty.vimusic.core.model.UserId

enum class LibraryTab {
    SONGS, FAVOURITES, MOST_PLAYED, RECENTLY_PLAYED, PLAYLISTS, OFFLINE, LOCAL;

    fun labelFor(userId: String): String = labelFor(UserId.isGuest(userId))

    fun labelFor(isGuest: Boolean): String = when (this) {
        SONGS -> "Songs"
        FAVOURITES -> "My Favourites"
        MOST_PLAYED -> if (isGuest) "Master's Mix" else "Most Played"
        RECENTLY_PLAYED -> "Recently Played"
        PLAYLISTS -> "Playlists"
        OFFLINE -> "Offline"
        LOCAL -> "Local"
    }

    fun shortLabelFor(isGuest: Boolean): String = when (this) {
        SONGS -> "Songs"
        FAVOURITES -> "Favourites"
        MOST_PLAYED -> if (isGuest) "Mix" else "Most played"
        RECENTLY_PLAYED -> "Recent"
        PLAYLISTS -> "Playlists"
        OFFLINE -> "Offline"
        LOCAL -> "Local"
    }

    val icon: ImageVector
        get() = when (this) {
            SONGS -> Icons.Rounded.MusicNote
            FAVOURITES -> Icons.Rounded.Favorite
            MOST_PLAYED -> Icons.Rounded.TrendingUp
            RECENTLY_PLAYED -> Icons.Rounded.History
            PLAYLISTS -> Icons.Rounded.QueueMusic
            OFFLINE -> Icons.Rounded.DownloadDone
            LOCAL -> Icons.Rounded.Folder
        }

    companion object {
        fun visibleFor(userId: String): List<LibraryTab> =
            if (UserId.isGuest(userId)) listOf(MOST_PLAYED, PLAYLISTS, LOCAL)
            else listOf(FAVOURITES, MOST_PLAYED, RECENTLY_PLAYED, PLAYLISTS, LOCAL)

        fun visibleFor(userId: String, resolved: Boolean): List<LibraryTab> =
            if (resolved) visibleFor(userId)
            else listOf(FAVOURITES, MOST_PLAYED, RECENTLY_PLAYED, PLAYLISTS, LOCAL)
    }
}

enum class SortKey(val label: String, val defaultAscending: Boolean) {
    ADDED_ON("Date added", false),
    TITLE("Title", true);

    val icon: ImageVector
        get() = when (this) {
            ADDED_ON -> Icons.Rounded.Schedule
            TITLE -> Icons.Rounded.SortByAlpha
        }
}

object LibraryFiltering {
    fun forConnectivity(songs: List<Song>, isOnline: Boolean): List<Song> =
        if (isOnline) songs else songs.filter { it.isPlayableOffline }

    fun search(songs: List<Song>, query: String): List<Song> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return songs
        return songs.filter {
            it.title.lowercase().contains(q) ||
                it.artistsText?.lowercase()?.contains(q) == true
        }
    }

    fun sortInPlaylist(songs: List<Song>, key: SortKey, ascending: Boolean): List<Song> =
        when (key) {
            SortKey.TITLE -> songs.sortedBy { it.title.lowercase() }
                .let { if (ascending) it else it.reversed() }
            SortKey.ADDED_ON -> if (ascending) songs else songs.reversed()
        }

    fun sort(songs: List<Song>, key: SortKey, ascending: Boolean): List<Song> {
        val sorted = when (key) {
            SortKey.TITLE -> songs.sortedBy { it.title.lowercase() }

            SortKey.ADDED_ON -> songs.sortedBy { it.likedAt ?: 0L }
        }
        return if (ascending) sorted else sorted.reversed()
    }
}
