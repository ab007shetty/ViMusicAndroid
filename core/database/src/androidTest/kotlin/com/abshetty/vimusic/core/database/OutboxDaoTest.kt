package com.abshetty.vimusic.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.abshetty.vimusic.core.database.dao.OutboxDao
import com.abshetty.vimusic.core.database.entity.OutboxEntity
import com.abshetty.vimusic.core.database.entity.OutboxOp
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class OutboxDaoTest {
    private lateinit var db: ViMusicDatabase
    private lateinit var dao: OutboxDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), ViMusicDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.outboxDao()
    }

    @After fun tearDown() = db.close()

    private fun entry(op: OutboxOp, payload: String, createdAt: Long) =
        OutboxEntity(op = op, payload = payload, createdAt = createdAt)

    @Test fun `pending returns entries oldest first`() = runTest {
        dao.enqueue(entry(OutboxOp.SET_LIKED, "b", createdAt = 200))
        dao.enqueue(entry(OutboxOp.SET_LIKED, "a", createdAt = 100))

        assertThat(dao.pending(10).map { it.payload }).containsExactly("a", "b").inOrder()
    }

    @Test fun `entries sharing a timestamp keep insertion order`() = runTest {
        dao.enqueue(entry(OutboxOp.CREATE_PLAYLIST, "first", createdAt = 500))
        dao.enqueue(entry(OutboxOp.RENAME_PLAYLIST, "second", createdAt = 500))

        assertThat(dao.pending(10).map { it.payload })
            .containsExactly("first", "second").inOrder()
    }

    @Test fun `pending respects the limit`() = runTest {
        repeat(5) { dao.enqueue(entry(OutboxOp.SET_LIKED, "p" + it, createdAt = it.toLong())) }

        assertThat(dao.pending(3)).hasSize(3)
    }

    @Test fun `delete removes a drained entry`() = runTest {
        dao.enqueue(entry(OutboxOp.SET_LIKED, "a", createdAt = 1))
        dao.delete(dao.pending(1).single().id)

        assertThat(dao.pending(10)).isEmpty()
    }

    @Test fun `recordFailure increments attempts and stores the error`() = runTest {
        dao.enqueue(entry(OutboxOp.SET_LIKED, "a", createdAt = 1))
        val id = dao.pending(1).single().id

        dao.recordFailure(id, "503 upstream")
        dao.recordFailure(id, "503 upstream")

        val row = dao.pending(1).single()
        assertThat(row.attempts).isEqualTo(2)
        assertThat(row.lastError).isEqualTo("503 upstream")
    }

    @Test fun `count reflects the queue depth`() = runTest {
        assertThat(dao.count()).isEqualTo(0)
        dao.enqueue(entry(OutboxOp.SET_LIKED, "a", createdAt = 1))
        assertThat(dao.count()).isEqualTo(1)
    }
}
