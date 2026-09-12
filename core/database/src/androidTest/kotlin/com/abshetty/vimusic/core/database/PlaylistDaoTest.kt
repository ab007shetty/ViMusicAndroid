package com.abshetty.vimusic.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.abshetty.vimusic.core.database.dao.PlaylistDao
import com.abshetty.vimusic.core.database.dao.SongDao
import com.abshetty.vimusic.core.database.entity.PlaylistEntity
import com.abshetty.vimusic.core.database.entity.SongEntity
import com.abshetty.vimusic.core.database.entity.SongPlaylistMapEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class PlaylistDaoTest {
    private lateinit var db: ViMusicDatabase
    private lateinit var dao: PlaylistDao
    private lateinit var songs: SongDao
    private val me = "me@example.com"

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), ViMusicDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.playlistDao()
        songs = db.songDao()
    }

    @After fun tearDown() = db.close()

    private suspend fun addSong(id: String) =
        songs.upsert(SongEntity(id = id, userId = me, title = "Title " + id))

    @Test fun `playlists are listed alphabetically ignoring case`() = runTest {
        dao.upsert(PlaylistEntity(userId = me, name = "zebra"))
        dao.upsert(PlaylistEntity(userId = me, name = "Apple"))

        assertThat(dao.playlists(me).first().map { it.name })
            .containsExactly("Apple", "zebra").inOrder()
    }

    @Test fun `songCount counts only live mappings`() = runTest {
        val pid = dao.upsert(PlaylistEntity(userId = me, name = "Mix"))
        addSong("a"); addSong("b")
        dao.addMapping(SongPlaylistMapEntity("a", pid, me, position = 1))
        dao.addMapping(SongPlaylistMapEntity("b", pid, me, position = 2, deletedLocally = true))

        assertThat(dao.playlists(me).first().single().songCount).isEqualTo(1)
    }

    @Test fun `songsIn preserves position ordering`() = runTest {
        val pid = dao.upsert(PlaylistEntity(userId = me, name = "Mix"))
        addSong("a"); addSong("b"); addSong("c")
        dao.addMapping(SongPlaylistMapEntity("c", pid, me, position = 1))
        dao.addMapping(SongPlaylistMapEntity("a", pid, me, position = 2))
        dao.addMapping(SongPlaylistMapEntity("b", pid, me, position = 3))

        assertThat(dao.songsIn(pid, me).first().map { it.id })
            .containsExactly("c", "a", "b").inOrder()
    }

    @Test fun `nextPosition starts at one and then increments`() = runTest {
        val pid = dao.upsert(PlaylistEntity(userId = me, name = "Mix"))
        assertThat(dao.nextPosition(pid, me)).isEqualTo(1)

        addSong("a")
        dao.addMapping(SongPlaylistMapEntity("a", pid, me, position = 1))
        assertThat(dao.nextPosition(pid, me)).isEqualTo(2)
    }

    @Test fun `markDeleted hides the playlist without dropping the row`() = runTest {
        val pid = dao.upsert(PlaylistEntity(userId = me, name = "Mix"))
        dao.markDeleted(pid, me)

        assertThat(dao.playlists(me).first()).isEmpty()
        assertThat(dao.findById(pid, me)).isNotNull()
        assertThat(dao.findById(pid, me)!!.deletedLocally).isTrue()
    }

    @Test fun `playlistIdsContaining reports membership`() = runTest {
        val p1 = dao.upsert(PlaylistEntity(userId = me, name = "One"))
        val p2 = dao.upsert(PlaylistEntity(userId = me, name = "Two"))
        addSong("a")
        dao.addMapping(SongPlaylistMapEntity("a", p1, me, position = 1))

        val ids = dao.playlistIdsContaining("a", me).first()
        assertThat(ids).containsExactly(p1)
        assertThat(ids).doesNotContain(p2)
    }

    @Test fun `removeMapping drops the song from the playlist`() = runTest {
        val pid = dao.upsert(PlaylistEntity(userId = me, name = "Mix"))
        addSong("a")
        dao.addMapping(SongPlaylistMapEntity("a", pid, me, position = 1))
        dao.removeMapping("a", pid, me)

        assertThat(dao.songsIn(pid, me).first()).isEmpty()
    }

    @Test fun `attachRemoteId records the server id and clears dirty`() = runTest {
        val pid = dao.upsert(PlaylistEntity(userId = me, name = "Mix", dirty = true))
        dao.attachRemoteId(pid, remoteId = 4242)

        val row = dao.findById(pid, me)!!
        assertThat(row.remoteId).isEqualTo(4242)
        assertThat(row.dirty).isFalse()
    }

    @Test fun `playlists are scoped to their owner`() = runTest {
        dao.upsert(PlaylistEntity(userId = me, name = "Mine"))
        dao.upsert(PlaylistEntity(userId = "other@example.com", name = "Theirs"))

        assertThat(dao.playlists(me).first().map { it.name }).containsExactly("Mine")
    }
}
