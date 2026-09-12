package com.abshetty.vimusic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abshetty.vimusic.core.database.entity.LyricsEntity

@Dao
interface LyricsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: LyricsEntity)

    @Query("SELECT * FROM lyrics WHERE songId = :songId AND userId = :userId")
    suspend fun find(songId: String, userId: String): LyricsEntity?
}
