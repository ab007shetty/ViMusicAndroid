package com.abshetty.vimusic.core.data.repository

import com.abshetty.vimusic.core.data.auth.AuthRepository
import com.abshetty.vimusic.core.data.sync.OutboxWriter
import com.abshetty.vimusic.core.data.sync.SyncScheduler
import com.abshetty.vimusic.core.database.dao.PlaylistDao
import com.abshetty.vimusic.core.database.dao.SongDao
import com.abshetty.vimusic.core.database.entity.OutboxOp
import com.abshetty.vimusic.core.database.entity.PlaylistEntity
import com.abshetty.vimusic.core.database.entity.SongEntity
import com.abshetty.vimusic.core.database.entity.SongPlaylistMapEntity
import com.abshetty.vimusic.core.model.Playlist
import com.abshetty.vimusic.core.model.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class PlaylistRepository @Inject constructor(
    private val dao: PlaylistDao,
    private val songDao: SongDao,
    private val outbox: OutboxWriter,
    private val auth: AuthRepository,
    private val sync: SyncScheduler,
) {
    fun playlists(): Flow<List<Playlist>> = auth.userId
        .flatMapLatest { dao.playlists(it) }
        .map { rows ->
            rows.map {
                Playlist(it.id, it.userId, it.name, it.browseId, it.songCount, it.coverUrl)
            }
        }

    fun songsIn(playlistId: Long): Flow<List<Song>> = auth.userId
        .flatMapLatest { dao.songsIn(playlistId, it) }
        .map { list -> list.map { it.toDomain() } }

    fun playlistIdsContaining(songId: String): Flow<List<Long>> = auth.userId
        .flatMapLatest { dao.playlistIdsContaining(songId, it) }

    suspend fun create(name: String): Long {
        val userId = auth.userId.value
        val localId = dao.upsert(PlaylistEntity(userId = userId, name = name.trim(), dirty = true))
        outbox.enqueue(userId, OutboxOp.CREATE_PLAYLIST, buildJsonObject {
            put("localId", localId); put("userId", userId); put("name", name.trim())
        })
        sync.requestSync()
        return localId
    }

    suspend fun setCover(playlistId: Long, url: String?) {
        val userId = auth.userId.value
        dao.setCover(playlistId, userId, url)
        outbox.enqueue(userId, OutboxOp.SET_PLAYLIST_COVER, buildJsonObject {
            put("localId", playlistId); put("userId", userId); put("coverUrl", url)
        })
        sync.requestSync()
    }

    suspend fun rename(playlistId: Long, name: String) {
        val userId = auth.userId.value
        dao.rename(playlistId, userId, name.trim())
        outbox.enqueue(userId, OutboxOp.RENAME_PLAYLIST, buildJsonObject {
            put("localId", playlistId); put("userId", userId); put("name", name.trim())
        })
        sync.requestSync()
    }

    suspend fun delete(playlistId: Long) {
        val userId = auth.userId.value
        dao.markDeleted(playlistId, userId)
        outbox.enqueue(userId, OutboxOp.DELETE_PLAYLIST, buildJsonObject {
            put("localId", playlistId); put("userId", userId)
        })
        sync.requestSync()
    }

    suspend fun addSong(playlistId: Long, song: Song) {
        val userId = auth.userId.value
        if (songDao.findById(song.id, userId) == null) {
            songDao.upsert(SongEntity.from(song.copy(userId = userId)))
        }
        val position = dao.nextPosition(playlistId, userId)
        dao.addMapping(SongPlaylistMapEntity(song.id, playlistId, userId, position, dirty = true))
        outbox.enqueue(userId, OutboxOp.ADD_SONG_TO_PLAYLIST, buildJsonObject {
            put("localId", playlistId); put("songId", song.id)
            put("userId", userId); put("position", position)
            put("title", song.title); put("artistsText", song.artistsText)
            put("durationText", song.durationText); put("thumbnailUrl", song.thumbnailUrl)
            put("channelId", song.channelId)
        })
        sync.requestSync()
    }

    suspend fun reorder(playlistId: Long, songIdsInOrder: List<String>) {
        val userId = auth.userId.value
        dao.reorder(playlistId, userId, songIdsInOrder)
        outbox.enqueue(userId, OutboxOp.REORDER_PLAYLIST, buildJsonObject {
            put("localId", playlistId)
            put("userId", userId)
            put("songIds", kotlinx.serialization.json.JsonArray(
                songIdsInOrder.map { kotlinx.serialization.json.JsonPrimitive(it) }
            ))
        })
        sync.requestSync()
    }

    suspend fun removeSong(playlistId: Long, songId: String) {
        val userId = auth.userId.value
        dao.removeMapping(songId, playlistId, userId)
        outbox.enqueue(userId, OutboxOp.REMOVE_SONG_FROM_PLAYLIST, buildJsonObject {
            put("localId", playlistId); put("songId", songId); put("userId", userId)
        })
        sync.requestSync()
    }
}
