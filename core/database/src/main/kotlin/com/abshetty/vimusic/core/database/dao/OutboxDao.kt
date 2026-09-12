package com.abshetty.vimusic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.abshetty.vimusic.core.database.entity.OutboxEntity

@Dao
interface OutboxDao {
    @Insert
    suspend fun enqueue(entry: OutboxEntity)

    @Query("SELECT * FROM sync_outbox ORDER BY createdAt ASC, id ASC LIMIT :limit")
    suspend fun pending(limit: Int): List<OutboxEntity>

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE sync_outbox SET attempts = attempts + 1, lastError = :error WHERE id = :id")
    suspend fun recordFailure(id: Long, error: String)

    @Query("SELECT COUNT(*) FROM sync_outbox")
    suspend fun count(): Int

    @Query("DELETE FROM sync_outbox WHERE payload LIKE '%\"local:%'")
    suspend fun deleteLocalTrackEntries(): Int
}
