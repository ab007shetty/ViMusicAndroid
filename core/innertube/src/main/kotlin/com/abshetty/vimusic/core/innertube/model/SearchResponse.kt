package com.abshetty.vimusic.core.innertube.model

data class SearchItem(
    val videoId: String,
    val title: String,
    val artistsText: String?,
    val durationText: String?,
    val thumbnailUrl: String?,
    val channelId: String? = null,
)

data class SearchResult(val items: List<SearchItem>, val continuation: String?)
