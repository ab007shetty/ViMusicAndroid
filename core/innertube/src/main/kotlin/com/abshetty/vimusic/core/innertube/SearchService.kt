package com.abshetty.vimusic.core.innertube

import com.abshetty.vimusic.core.innertube.model.SearchResult

class SearchService constructor(private val client: InnertubeClient) {
    suspend fun search(
        query: String,
        filter: SearchFilter = SearchFilter.SONGS,
        continuation: String? = null,
    ): Result<SearchResult> = runCatching {
        SearchParser.parse(
            client.search(query, filter.param, continuation, ClientContext.WEB_REMIX)
        )
    }

    suspend fun suggestions(query: String): Result<List<String>> = runCatching {
        if (query.isBlank()) emptyList()
        else client.searchSuggestions(query, ClientContext.WEB_REMIX)
    }
}
