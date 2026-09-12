package com.abshetty.vimusic.feature.search

import com.abshetty.vimusic.core.innertube.SearchFilter
import com.abshetty.vimusic.core.innertube.YouTubeUrlParser
import com.abshetty.vimusic.core.innertube.model.SearchItem
import com.abshetty.vimusic.core.innertube.model.SearchResult

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter.SONGS,
    val items: List<SearchItem> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val history: List<String> = emptyList(),

    val isGuest: Boolean = true,

    val favouriteIds: Set<String> = emptySet(),
    val continuation: String? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val isOnline: Boolean = true,
)

object SearchRouting {
    sealed interface Decision {
        data class DirectVideo(val videoId: String, val isShort: Boolean) : Decision
        data class TextSearch(val query: String) : Decision
        data object Ignore : Decision
    }

    fun route(input: String): Decision {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return Decision.Ignore

        YouTubeUrlParser.parse(trimmed)?.let {
            return Decision.DirectVideo(it.videoId, it.isShort)
        }
        return Decision.TextSearch(trimmed)
    }
}

object SearchPaging {
    fun merge(existing: List<SearchItem>, incoming: List<SearchItem>): List<SearchItem> {
        val seen = existing.mapTo(mutableSetOf()) { it.videoId }
        return existing + incoming.filter { seen.add(it.videoId) }
    }

    fun canLoadMore(result: SearchResult): Boolean = result.continuation != null
}
