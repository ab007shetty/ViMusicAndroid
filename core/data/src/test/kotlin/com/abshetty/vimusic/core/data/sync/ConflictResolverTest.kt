package com.abshetty.vimusic.core.data.sync

import com.abshetty.vimusic.core.database.entity.SongEntity
import com.abshetty.vimusic.core.model.CacheState
import com.abshetty.vimusic.core.model.DownloadState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConflictResolverTest {
    private fun song(
        likedAt: Long? = null,
        lastPlayedAt: Long? = null,
        totalPlayTimeMs: Long = 0,
        cacheState: CacheState = CacheState.NONE,
        downloadState: DownloadState = DownloadState.NONE,
        title: String = "T",
        thumbnailUrl: String? = null,
    ) = SongEntity(
        id = "a", userId = "me@example.com", title = title,
        likedAt = likedAt, lastPlayedAt = lastPlayedAt, totalPlayTimeMs = totalPlayTimeMs,
        cacheState = cacheState, downloadState = downloadState, thumbnailUrl = thumbnailUrl,
    )

    @Test fun `newer likedAt wins regardless of side`() {
        assertThat(ConflictResolver.resolve(song(likedAt = 100), song(likedAt = 200)).likedAt)
            .isEqualTo(200)
        assertThat(ConflictResolver.resolve(song(likedAt = 300), song(likedAt = 200)).likedAt)
            .isEqualTo(300)
    }

    @Test fun `an unlike is honoured when the local side is newer`() {
        val local = song(likedAt = null, lastPlayedAt = 500)
        val remote = song(likedAt = 100, lastPlayedAt = 100)

        assertThat(ConflictResolver.resolve(local, remote).likedAt).isNull()
    }

    @Test fun `a remote like wins when the remote side is newer`() {
        val local = song(likedAt = null, lastPlayedAt = 100)
        val remote = song(likedAt = 900, lastPlayedAt = 900)

        assertThat(ConflictResolver.resolve(local, remote).likedAt).isEqualTo(900)
    }

    @Test fun `play time takes the larger value because the RPC is additive`() {
        assertThat(
            ConflictResolver.resolve(song(totalPlayTimeMs = 5_000), song(totalPlayTimeMs = 9_000))
                .totalPlayTimeMs
        ).isEqualTo(9_000)
    }

    @Test fun `play time never regresses when local is ahead`() {
        assertThat(
            ConflictResolver.resolve(song(totalPlayTimeMs = 9_000), song(totalPlayTimeMs = 1_000))
                .totalPlayTimeMs
        ).isEqualTo(9_000)
    }

    @Test fun `lastPlayedAt takes the more recent`() {
        assertThat(ConflictResolver.resolve(song(lastPlayedAt = 10), song(lastPlayedAt = 90)).lastPlayedAt)
            .isEqualTo(90)
    }

    @Test fun `a null lastPlayedAt on one side does not erase the other`() {
        assertThat(ConflictResolver.resolve(song(lastPlayedAt = 10), song(lastPlayedAt = null)).lastPlayedAt)
            .isEqualTo(10)
        assertThat(ConflictResolver.resolve(song(lastPlayedAt = null), song(lastPlayedAt = 10)).lastPlayedAt)
            .isEqualTo(10)
    }

    @Test fun `local cache and download state always survive`() {
        val local = song(cacheState = CacheState.CACHED, downloadState = DownloadState.DOWNLOADED)
        val remote = song(cacheState = CacheState.NONE, downloadState = DownloadState.NONE)

        val merged = ConflictResolver.resolve(local, remote)

        assertThat(merged.cacheState).isEqualTo(CacheState.CACHED)
        assertThat(merged.downloadState).isEqualTo(DownloadState.DOWNLOADED)
    }

    @Test fun `remote metadata fills gaps without blanking local values`() {
        val local = song(title = "Local Title", thumbnailUrl = "local.jpg")
        val remote = song(title = "", thumbnailUrl = null)

        val merged = ConflictResolver.resolve(local, remote)

        assertThat(merged.title).isEqualTo("Local Title")
        assertThat(merged.thumbnailUrl).isEqualTo("local.jpg")
    }

    @Test fun `merging clears the dirty flag`() {
        val merged = ConflictResolver.resolve(song().copy(dirty = true), song())
        assertThat(merged.dirty).isFalse()
    }

    @Test fun `merging is stable when both sides are identical`() {
        val s = song(likedAt = 5, lastPlayedAt = 5, totalPlayTimeMs = 5)
        assertThat(ConflictResolver.resolve(s, s)).isEqualTo(s)
    }
}
