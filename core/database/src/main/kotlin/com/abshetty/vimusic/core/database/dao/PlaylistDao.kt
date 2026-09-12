package com.abshetty.vimusic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.abshetty.vimusic.core.database.entity.PlaylistEntity
import com.abshetty.vimusic.core.database.entity.SongEntity
import com.abshetty.vimusic.core.database.entity.SongPlaylistMapEntity
import kotlinx.coroutines.flow.Flow

data class PlaylistWithCount(
    val id: Long,
    val userId: String,
    val name: String,
    val browseId: String?,
    val coverUrl: String?,
    val songCount: Int,
)

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlist WHERE id = :id AND userId = :userId")
    suspend fun findById(id: Long, userId: String): PlaylistEntity?

    @Query("SELECT * FROM playlist WHERE remoteId = :remoteId AND userId = :userId LIMIT 1")
    suspend fun findByRemoteId(remoteId: Long, userId: String): PlaylistEntity?

    @Query("SELECT * FROM playlist WHERE name = :name AND userId = :userId LIMIT 1")
    suspend fun findByName(name: String, userId: String): PlaylistEntity?

    @Query("""
        SELECT p.id AS id, p.userId AS userId, p.name AS name, p.browseId AS browseId,
               p.coverUrl AS coverUrl,
               (SELECT COUNT(*) FROM song_playlist_map m
                WHERE m.playlistId = p.id AND m.deletedLocally = 0) AS songCount
        FROM playlist p
        WHERE p.userId = :userId AND p.deletedLocally = 0
        ORDER BY p.name COLLATE NOCASE ASC
    """)
    fun playlists(userId: String): Flow<List<PlaylistWithCount>>

    @Transaction
    @Query("""
        SELECT s.* FROM song s
        INNER JOIN song_playlist_map m ON m.songId = s.id AND m.userId = s.userId
        WHERE m.playlistId = :playlistId AND m.userId = :userId AND m.deletedLocally = 0
        ORDER BY m.position ASC
    """)
    fun songsIn(playlistId: Long, userId: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM song_playlist_map WHERE userId = :userId")
    suspend fun mappingSnapshot(userId: String): List<SongPlaylistMapEntity>

    @Query("SELECT playlistId FROM song_playlist_map WHERE songId = :songId AND userId = :userId AND deletedLocally = 0")
    fun playlistIdsContaining(songId: String, userId: String): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMapping(mapping: SongPlaylistMapEntity)

    @Query("SELECT COALESCE(MAX(position), 0) + 1 FROM song_playlist_map WHERE playlistId = :playlistId AND userId = :userId")
    suspend fun nextPosition(playlistId: Long, userId: String): Int

    @Query("DELETE FROM song_playlist_map WHERE songId = :songId AND playlistId = :playlistId AND userId = :userId")
    suspend fun removeMapping(songId: String, playlistId: Long, userId: String)

    @Query("UPDATE playlist SET name = :name, dirty = 1 WHERE id = :id AND userId = :userId")
    suspend fun rename(id: Long, userId: String, name: String)

    @Query("UPDATE playlist SET deletedLocally = 1, dirty = 1 WHERE id = :id AND userId = :userId")
    suspend fun markDeleted(id: Long, userId: String)

    @Query("UPDATE playlist SET coverUrl = :url WHERE id = :id AND userId = :userId")
    suspend fun setCover(id: Long, userId: String, url: String?)

    @Query("UPDATE playlist SET remoteId = :remoteId, dirty = 0 WHERE id = :localId")
    suspend fun attachRemoteId(localId: Long, remoteId: Long)

    @Query("DELETE FROM playlist WHERE id = :id")
    suspend fun deleteRow(id: Long)

    @Query("DELETE FROM song_playlist_map WHERE playlistId = :playlistId")
    suspend fun deleteMappingsFor(playlistId: Long)

    @Query("UPDATE song_playlist_map SET position = :position WHERE playlistId = :playlistId AND songId = :songId AND userId = :userId")
    suspend fun setPosition(playlistId: Long, songId: String, userId: String, position: Int)

    @Transaction
    suspend fun reorder(playlistId: Long, userId: String, songIdsInOrder: List<String>) {
        songIdsInOrder.forEachIndexed { index, songId ->
            setPosition(playlistId, songId, userId, index)
        }
    }
}
