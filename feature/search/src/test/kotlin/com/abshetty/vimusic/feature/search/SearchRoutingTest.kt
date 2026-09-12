package com.abshetty.vimusic.feature.search

import com.abshetty.vimusic.core.innertube.SearchFilter
import com.abshetty.vimusic.core.innertube.UrlSource
import com.abshetty.vimusic.core.innertube.YouTubeUrlParser
import com.abshetty.vimusic.core.innertube.model.SearchItem
import com.abshetty.vimusic.core.innertube.model.SearchResult
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SearchRoutingTest {
    private fun item(id: String) = SearchItem(
        videoId = id, title = "Song " + id, artistsText = "Artist",
        durationText = "3:00", thumbnailUrl = "https://img/" + id,
    )

    @Test fun `a pasted url is routed to direct resolution not a text search`() {
        val decision = SearchRouting.route("https://youtu.be/TUVcZfQe-Kw")

        assertThat(decision).isInstanceOf(SearchRouting.Decision.DirectVideo::class.java)
        assertThat((decision as SearchRouting.Decision.DirectVideo).videoId)
            .isEqualTo("TUVcZfQe-Kw")
    }

    @Test fun `a plain query is routed to a text search`() {
        assertThat(SearchRouting.route("m83 midnight city"))
            .isEqualTo(SearchRouting.Decision.TextSearch("m83 midnight city"))
    }

    @Test fun `a blank query is ignored`() {
        assertThat(SearchRouting.route("   ")).isEqualTo(SearchRouting.Decision.Ignore)
    }

    @Test fun `routing trims the query`() {
        assertThat(SearchRouting.route("  m83  "))
            .isEqualTo(SearchRouting.Decision.TextSearch("m83"))
    }

    @Test fun `a shorts url is recognised and flagged`() {
        val parsed = YouTubeUrlParser.parse("https://youtube.com/shorts/abc123XYZ_-")
        assertThat(parsed?.isShort).isTrue()
        assertThat(parsed?.source).isEqualTo(UrlSource.YOUTUBE)
    }

    @Test fun `loading more appends without duplicating existing items`() {
        val merged = SearchPaging.merge(
            listOf(item("a"), item("b")),
            listOf(item("b"), item("c")),
        )

        assertThat(merged.map { it.videoId }).containsExactly("a", "b", "c").inOrder()
    }

    @Test fun `merging preserves the order of the first occurrence`() {
        val merged = SearchPaging.merge(listOf(item("x")), listOf(item("y"), item("x")))

        assertThat(merged.map { it.videoId }).containsExactly("x", "y").inOrder()
    }

    @Test fun `merging into an empty list keeps the incoming order`() {
        val merged = SearchPaging.merge(emptyList(), listOf(item("a"), item("b")))
        assertThat(merged.map { it.videoId }).containsExactly("a", "b").inOrder()
    }

    @Test fun `a null continuation means there is nothing more to load`() {
        assertThat(SearchPaging.canLoadMore(SearchResult(emptyList(), continuation = null)))
            .isFalse()
        assertThat(SearchPaging.canLoadMore(SearchResult(emptyList(), continuation = "tok")))
            .isTrue()
    }

    @Test fun `the default filter is songs`() {
        assertThat(SearchUiState().filter).isEqualTo(SearchFilter.SONGS)
    }
}
