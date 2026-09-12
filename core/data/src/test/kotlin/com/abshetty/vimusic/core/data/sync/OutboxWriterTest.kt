package com.abshetty.vimusic.core.data.sync

import com.abshetty.vimusic.core.database.dao.OutboxDao
import com.abshetty.vimusic.core.database.entity.OutboxEntity
import com.abshetty.vimusic.core.database.entity.OutboxOp
import com.abshetty.vimusic.core.model.UserId
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Test

private class FakeOutboxDao : OutboxDao {
    val entries = mutableListOf<OutboxEntity>()
    override suspend fun enqueue(entry: OutboxEntity) { entries += entry }
    override suspend fun pending(limit: Int) = entries.take(limit)
    override suspend fun delete(id: Long) { entries.removeAll { it.id == id } }
    override suspend fun recordFailure(id: Long, error: String) = Unit
    override suspend fun count() = entries.size
    override suspend fun deleteLocalTrackEntries(): Int {
        val before = entries.size
        entries.removeAll { it.payload.contains("\"local:") }
        return before - entries.size
    }
}

class OutboxWriterTest {
    private val payload = buildJsonObject { put("songId", "abc") }

    @Test fun `enqueues writes for a signed-in user`() = runTest {
        val dao = FakeOutboxDao()
        OutboxWriter(dao) { 1000L }.enqueue("me@example.com", OutboxOp.SET_LIKED, payload)

        assertThat(dao.entries).hasSize(1)
        assertThat(dao.entries.single().op).isEqualTo(OutboxOp.SET_LIKED)
    }

    @Test fun `silently drops writes for the guest bucket`() = runTest {
        val dao = FakeOutboxDao()
        OutboxWriter(dao) { 1000L }.enqueue(UserId.GUEST, OutboxOp.SET_LIKED, payload)

        assertThat(dao.entries).isEmpty()
    }

    @Test fun `drops guest writes for every op, not just likes`() = runTest {
        val dao = FakeOutboxDao()
        val writer = OutboxWriter(dao) { 0L }
        OutboxOp.entries.forEach { writer.enqueue(UserId.GUEST, it, payload) }

        assertThat(dao.entries).isEmpty()
    }

    @Test fun `stamps the entry with the injected clock`() = runTest {
        val dao = FakeOutboxDao()
        OutboxWriter(dao) { 4242L }.enqueue("me@example.com", OutboxOp.SET_LIKED, payload)

        assertThat(dao.entries.single().createdAt).isEqualTo(4242L)
    }

    @Test fun `serializes the payload to json text`() = runTest {
        val dao = FakeOutboxDao()
        OutboxWriter(dao) { 0L }.enqueue("me@example.com", OutboxOp.SET_LIKED, payload)

        assertThat(dao.entries.single().payload).contains("songId")
        assertThat(dao.entries.single().payload).contains("abc")
    }

    @Test fun `preserves enqueue order for the same user`() = runTest {
        val dao = FakeOutboxDao()
        val writer = OutboxWriter(dao) { 0L }
        writer.enqueue("me@example.com", OutboxOp.CREATE_PLAYLIST, payload)
        writer.enqueue("me@example.com", OutboxOp.ADD_SONG_TO_PLAYLIST, payload)

        assertThat(dao.entries.map { it.op })
            .containsExactly(OutboxOp.CREATE_PLAYLIST, OutboxOp.ADD_SONG_TO_PLAYLIST).inOrder()
    }
}
