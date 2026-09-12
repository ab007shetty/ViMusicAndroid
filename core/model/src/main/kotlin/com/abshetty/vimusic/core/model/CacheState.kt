package com.abshetty.vimusic.core.model

enum class CacheState { NONE, PARTIAL, CACHED }

enum class DownloadState { NONE, QUEUED, DOWNLOADING, DOWNLOADED, FAILED }

fun playableOffline(cache: CacheState, download: DownloadState): Boolean =
    cache == CacheState.CACHED || download == DownloadState.DOWNLOADED
