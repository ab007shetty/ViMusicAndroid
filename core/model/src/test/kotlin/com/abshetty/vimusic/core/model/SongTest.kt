package com.abshetty.vimusic.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SongTest {
    private fun song(
        likedAt: Long? = null,
        cache: CacheState = CacheState.NONE,
        download: DownloadState = DownloadState.NONE,
    ) = Song(id = "a", userId = "me@example.com", title = "T",
             likedAt = likedAt, cacheState = cache, downloadState = download)

    @Test fun `isFavourite tracks likedAt`() {
        assertThat(song(likedAt = 1).isFavourite).isTrue()
        assertThat(song(likedAt = null).isFavourite).isFalse()
    }

    @Test fun `partial cache does not count as playable offline`() {
        assertThat(song(cache = CacheState.PARTIAL).isPlayableOffline).isFalse()
    }

    @Test fun `a fully cached track is playable offline`() {
        assertThat(song(cache = CacheState.CACHED).isPlayableOffline).isTrue()
    }

    @Test fun `a downloaded track is playable offline even with no cache`() {
        assertThat(song(download = DownloadState.DOWNLOADED).isPlayableOffline).isTrue()
    }

    @Test fun `an in-progress download is not yet playable offline`() {
        assertThat(song(download = DownloadState.DOWNLOADING).isPlayableOffline).isFalse()
        assertThat(song(download = DownloadState.QUEUED).isPlayableOffline).isFalse()
        assertThat(song(download = DownloadState.FAILED).isPlayableOffline).isFalse()
    }
}
