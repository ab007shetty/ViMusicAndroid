package com.abshetty.vimusic.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.abshetty.vimusic.core.database.dao.SongDao
import com.abshetty.vimusic.core.database.entity.SongEntity
import com.abshetty.vimusic.core.model.CacheState
import com.abshetty.vimusic.core.model.DownloadState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class SongDaoTest {
    private lateinit var db: ViMusicDatabase
    private lateinit var dao: SongDao
    private val me = "me@example.com"

    private fun song(
        id: String,
        userId: String = "me@example.com",
        likedAt: Long? = null,
        totalPlayTimeMs: Long = 0,
        lastPlayedAt: Long? = null,
    ) = SongEntity(
        id = id, userId = userId, title = "Title " + id, artistsText = "Artist",
        durationText = "3:00", thumbnailUrl = null, channelId = null,
        likedAt = likedAt, totalPlayTimeMs = totalPlayTimeMs, lastPlayedAt = lastPlayedAt,
    )

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), ViMusicDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.songDao()
    }

    @After fun tearDown() = db.close()

    @Test fun `favourites returns only liked songs newest first`() = runTest {
        dao.upsert(song("a", likedAt = 100))
        dao.upsert(song("b", likedAt = 300))
        dao.upsert(song("c", likedAt = null))

        assertThat(dao.favourites(me).first().map { it.id })
            .containsExactly("b", "a").inOrder()
    }

    @Test fun `mostPlayed excludes songs with zero play time`() = runTest {
        dao.upsert(song("a", totalPlayTimeMs = 5000))
        dao.upsert(song("b", totalPlayTimeMs = 0))

        assertThat(dao.mostPlayed(me).first().map { it.id }).containsExactly("a")
    }

    @Test fun `recentlyPlayed excludes songs never played`() = runTest {
        dao.upsert(song("a", lastPlayedAt = 999))
        dao.upsert(song("b", lastPlayedAt = null))

        assertThat(dao.recentlyPlayed(me).first().map { it.id }).containsExactly("a")
    }

    @Test fun `same song id for different users are separate rows`() = runTest {
        dao.upsert(song("a", userId = me, likedAt = 1))
        dao.upsert(song("a", userId = "", likedAt = 2))

        assertThat(dao.favourites(me).first()).hasSize(1)
        assertThat(dao.favourites("").first()).hasSize(1)
    }

    @Test fun `upsert replaces an existing row for the same key`() = runTest {
        dao.upsert(song("a", likedAt = 1))
        dao.upsert(song("a", likedAt = 2))

        val result = dao.favourites(me).first()
        assertThat(result).hasSize(1)
        assertThat(result.single().likedAt).isEqualTo(2)
    }

    @Test fun `setLikedAt to null unfavourites without deleting the row`() = runTest {
        dao.upsert(song("a", likedAt = 1))
        dao.setLikedAt("a", me, null)

        assertThat(dao.favourites(me).first()).isEmpty()
        assertThat(dao.findById("a", me)).isNotNull()
    }

    @Test fun `setCacheState updates only the cache column`() = runTest {
        dao.upsert(song("a", likedAt = 7))
        dao.setCacheState("a", me, CacheState.CACHED)

        val row = dao.findById("a", me)!!
        assertThat(row.cacheState).isEqualTo(CacheState.CACHED)
        assertThat(row.likedAt).isEqualTo(7)
    }

    @Test fun `addPlayTime accumulates rather than overwriting`() = runTest {
        dao.upsert(song("a", totalPlayTimeMs = 1_000))
        dao.addPlayTime("a", me, 2_500, playedAt = 42)

        val row = dao.findById("a", me)!!
        assertThat(row.totalPlayTimeMs).isEqualTo(3_500)
        assertThat(row.lastPlayedAt).isEqualTo(42)
    }

    @Test fun `offlinePlayable returns cached or downloaded only`() = runTest {
        dao.upsert(song("a"))
        dao.upsert(song("b"))
        dao.upsert(song("c"))
        dao.setCacheState("a", me, CacheState.CACHED)
        dao.setCacheState("b", me, CacheState.PARTIAL)
        dao.setDownloadState("c", me, DownloadState.DOWNLOADED)

        assertThat(dao.offlinePlayable(me).first().map { it.id }).containsExactly("a", "c")
    }

    @Test fun `queries are scoped to the requesting user`() = runTest {
        dao.upsert(song("a", userId = me, likedAt = 1))
        dao.upsert(song("b", userId = "other@example.com", likedAt = 1))

        assertThat(dao.favourites(me).first().map { it.id }).containsExactly("a")
    }
}
