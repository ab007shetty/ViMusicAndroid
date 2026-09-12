package com.abshetty.vimusic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.abshetty.vimusic.core.database.entity.SongEntity
import com.abshetty.vimusic.core.model.CacheState
import com.abshetty.vimusic.core.model.DownloadState
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("SELECT * FROM song WHERE id = :id AND userId = :userId")
    suspend fun findById(id: String, userId: String): SongEntity?

    @Query("SELECT * FROM song WHERE userId = :userId")
    suspend fun snapshot(userId: String): List<SongEntity>

    @Query("SELECT * FROM song WHERE userId = :userId ORDER BY title COLLATE NOCASE ASC")
    fun allSongs(userId: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE userId = :userId AND likedAt IS NOT NULL ORDER BY likedAt DESC")
    fun favourites(userId: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE userId = :userId AND totalPlayTimeMs > 0 ORDER BY totalPlayTimeMs DESC")
    fun mostPlayed(userId: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM song WHERE userId = :userId AND lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC")
    fun recentlyPlayed(userId: String): Flow<List<SongEntity>>

    @Query("UPDATE song SET likedAt = :likedAt WHERE id = :id AND userId = :userId")
    suspend fun setLikedAt(id: String, userId: String, likedAt: Long?)

    @Query("UPDATE song SET cacheState = :state WHERE id = :id AND userId = :userId")
    suspend fun setCacheState(id: String, userId: String, state: CacheState)

    @Query("UPDATE song SET cacheState = :state WHERE id = :id")
    suspend fun setCacheStateForAllUsers(id: String, state: CacheState)

    @Query("UPDATE song SET downloadState = :state WHERE id = :id AND userId = :userId")
    suspend fun setDownloadState(id: String, userId: String, state: DownloadState)

    @Query("UPDATE song SET totalPlayTimeMs = totalPlayTimeMs + :deltaMs, lastPlayedAt = :playedAt WHERE id = :id AND userId = :userId")
    suspend fun addPlayTime(id: String, userId: String, deltaMs: Long, playedAt: Long)

    @Query("SELECT * FROM song WHERE userId = :userId AND (cacheState = 'CACHED' OR downloadState = 'DOWNLOADED')")
    fun offlinePlayable(userId: String): Flow<List<SongEntity>>

    @Query("DELETE FROM song WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("DELETE FROM song WHERE id = :id AND userId = :userId")
    suspend fun deleteRow(id: String, userId: String)

    @Query("DELETE FROM song WHERE id LIKE 'local:%'")
    suspend fun deleteLocalTracks(): Int
}
