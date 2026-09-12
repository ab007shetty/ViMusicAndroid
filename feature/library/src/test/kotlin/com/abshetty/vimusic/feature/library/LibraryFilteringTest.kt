package com.abshetty.vimusic.feature.library

import com.abshetty.vimusic.core.model.CacheState
import com.abshetty.vimusic.core.model.Song
import com.abshetty.vimusic.core.model.UserId
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LibraryFilteringTest {
    private fun song(id: String, cached: Boolean = false) = Song(
        id = id, userId = "me@example.com", title = "Song " + id,
        cacheState = if (cached) CacheState.CACHED else CacheState.NONE,
    )

    @Test fun `a guest sees only the tabs that mean anything to them`() {
        assertThat(LibraryTab.visibleFor(UserId.GUEST)).containsExactly(
            LibraryTab.MOST_PLAYED, LibraryTab.PLAYLISTS, LibraryTab.LOCAL,
        ).inOrder()
    }

    @Test fun `a signed-in user sees every browsing section`() {
        assertThat(LibraryTab.visibleFor("me@example.com")).containsExactly(
            LibraryTab.FAVOURITES, LibraryTab.MOST_PLAYED,
            LibraryTab.RECENTLY_PLAYED, LibraryTab.PLAYLISTS, LibraryTab.LOCAL,
        ).inOrder()
    }

    @Test fun `local files are reachable without an account`() {
        assertThat(LibraryTab.visibleFor(UserId.GUEST)).contains(LibraryTab.LOCAL)
    }

    @Test fun `offline is no longer offered`() {
        assertThat(LibraryTab.visibleFor(UserId.GUEST)).doesNotContain(LibraryTab.OFFLINE)
        assertThat(LibraryTab.visibleFor("me@example.com")).doesNotContain(LibraryTab.OFFLINE)
    }

    @Test fun `the songs section exists but is not offered on the rail`() {
        assertThat(LibraryTab.entries).contains(LibraryTab.SONGS)
        assertThat(LibraryTab.visibleFor("me@example.com")).doesNotContain(LibraryTab.SONGS)
    }

    @Test fun `the most-played tab is labelled Master's Mix for a guest`() {
        assertThat(LibraryTab.MOST_PLAYED.labelFor(UserId.GUEST)).isEqualTo("Master's Mix")
        assertThat(LibraryTab.MOST_PLAYED.labelFor("me@example.com")).isEqualTo("Most Played")
    }

    @Test fun `offline hides songs that cannot actually play`() {
        val filtered = LibraryFiltering.forConnectivity(
            listOf(song("a", cached = true), song("b", cached = false)), isOnline = false
        )
        assertThat(filtered.map { it.id }).containsExactly("a")
    }

    @Test fun `online shows everything`() {
        val filtered = LibraryFiltering.forConnectivity(
            listOf(song("a", cached = true), song("b", cached = false)), isOnline = true
        )
        assertThat(filtered).hasSize(2)
    }

    @Test fun `local search matches title and artist case-insensitively`() {
        val songs = listOf(
            song("a").copy(title = "Midnight City", artistsText = "M83"),
            song("b").copy(title = "Outro", artistsText = "M83"),
            song("c").copy(title = "Nightcall", artistsText = "Kavinsky"),
        )

        assertThat(LibraryFiltering.search(songs, "midnight").map { it.id }).containsExactly("a")
        assertThat(LibraryFiltering.search(songs, "M83").map { it.id }).containsExactly("a", "b")
        assertThat(LibraryFiltering.search(songs, "").map { it.id })
            .containsExactly("a", "b", "c")
    }

    @Test fun `search ignores surrounding whitespace`() {
        val songs = listOf(song("a").copy(title = "Midnight City"))
        assertThat(LibraryFiltering.search(songs, "  midnight  ")).hasSize(1)
    }

    @Test fun `sorting by title respects the direction`() {
        val songs = listOf(song("a").copy(title = "Zebra"), song("b").copy(title = "apple"))

        assertThat(LibraryFiltering.sort(songs, SortKey.TITLE, ascending = true).map { it.id })
            .containsExactly("b", "a").inOrder()
        assertThat(LibraryFiltering.sort(songs, SortKey.TITLE, ascending = false).map { it.id })
            .containsExactly("a", "b").inOrder()
    }

    @Test fun `sorting by added-on leaves an undated song where it was found`() {
        val songs = listOf(
            song("a").copy(likedAt = null, totalPlayTimeMs = 900_000_000),
            song("b").copy(likedAt = 1_700_000_000_000, totalPlayTimeMs = 0),
        )

        assertThat(LibraryFiltering.sort(songs, SortKey.ADDED_ON, ascending = false).map { it.id })
            .containsExactly("b", "a").inOrder()
    }

    @Test fun `sorting an empty list is safe`() {
        assertThat(LibraryFiltering.sort(emptyList(), SortKey.TITLE, ascending = true)).isEmpty()
    }
}

class PlaylistOrderingTest {
    private fun song(id: String, likedAt: Long?, playTime: Long = 0) = Song(
        id = id,
        userId = "me@example.com",
        title = id,
        likedAt = likedAt,
        totalPlayTimeMs = playTime,
    )

    private val asStored = listOf(
        song("first", likedAt = null, playTime = 5_000_000),
        song("second", likedAt = 1_700_000_000_000),
        song("justAdded", likedAt = null),
    )

    @Test
    fun `newest first is the stored order reversed, whatever is favourited`() {
        val order = LibraryFiltering
            .sortInPlaylist(asStored, SortKey.ADDED_ON, ascending = false)
            .map { it.id }

        assertThat(order).containsExactly("justAdded", "second", "first").inOrder()
    }

    @Test
    fun `oldest first is the stored order untouched`() {
        val order = LibraryFiltering
            .sortInPlaylist(asStored, SortKey.ADDED_ON, ascending = true)
            .map { it.id }

        assertThat(order).containsExactly("first", "second", "justAdded").inOrder()
    }

    @Test
    fun `a song with no date does not sort by how long it has been played`() {
        val heavilyPlayed = song("played", likedAt = null, playTime = 900_000_000)
        val neverPlayed = song("never", likedAt = null)

        val order = LibraryFiltering
            .sort(listOf(heavilyPlayed, neverPlayed), SortKey.ADDED_ON, ascending = true)
            .map { it.id }

        assertThat(order).containsExactly("played", "never").inOrder()
    }
}
