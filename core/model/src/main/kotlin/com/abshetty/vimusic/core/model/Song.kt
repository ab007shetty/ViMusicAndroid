package com.abshetty.vimusic.core.model

data class Song(
    val id: String,
    val userId: String,
    val title: String,
    val artistsText: String? = null,
    val durationText: String? = null,
    val thumbnailUrl: String? = null,
    val channelId: String? = null,
    val likedAt: Long? = null,
    val totalPlayTimeMs: Long = 0,
    val lastPlayedAt: Long? = null,
    val cacheState: CacheState = CacheState.NONE,
    val downloadState: DownloadState = DownloadState.NONE,
) {
    val isFavourite: Boolean get() = likedAt != null
    val isPlayableOffline: Boolean get() = playableOffline(cacheState, downloadState)
}
