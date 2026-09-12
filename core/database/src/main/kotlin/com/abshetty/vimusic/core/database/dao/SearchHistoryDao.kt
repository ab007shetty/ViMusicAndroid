package com.abshetty.vimusic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abshetty.vimusic.core.database.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun record(entry: SearchHistoryEntity)

    @Query(
        "SELECT * FROM search_history WHERE userId = :userId " +
            "ORDER BY timestamp DESC LIMIT :limit"
    )
    fun recent(userId: String, limit: Int = 20): Flow<List<SearchHistoryEntity>>

    @Query("DELETE FROM search_history WHERE query = :query AND userId = :userId")
    suspend fun remove(query: String, userId: String)

    @Query("DELETE FROM search_history WHERE userId = :userId")
    suspend fun clear(userId: String)

    @Query("SELECT * FROM search_history WHERE userId = :userId")
    suspend fun snapshot(userId: String): List<SearchHistoryEntity>

    @androidx.room.Transaction
    suspend fun replaceAll(userId: String, entries: List<SearchHistoryEntity>) {
        val existing = snapshot(userId).associateBy { it.query }
        val wanted = entries.associateBy { it.query }
        if (existing == wanted) return

        existing.keys.filterNot { it in wanted }.forEach { remove(it, userId) }
        wanted.values.filter { existing[it.query] != it }.forEach { record(it) }
    }
}
