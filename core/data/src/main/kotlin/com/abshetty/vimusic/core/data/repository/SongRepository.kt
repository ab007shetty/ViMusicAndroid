package com.abshetty.vimusic.core.data.repository

import com.abshetty.vimusic.core.data.auth.AuthRepository
import com.abshetty.vimusic.core.data.sync.OutboxWriter
import com.abshetty.vimusic.core.data.sync.SyncScheduler
import com.abshetty.vimusic.core.database.dao.SongDao
import com.abshetty.vimusic.core.database.entity.OutboxOp
import com.abshetty.vimusic.core.database.entity.SongEntity
import com.abshetty.vimusic.core.model.CacheState
import com.abshetty.vimusic.core.model.LocalId
import com.abshetty.vimusic.core.model.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SongRepository @Inject constructor(
    private val dao: SongDao,
    private val outbox: OutboxWriter,
    private val auth: AuthRepository,
    private val sync: SyncScheduler,
) {
    fun allSongs(): Flow<List<Song>> = auth.userId
        .flatMapLatest { dao.allSongs(it) }.map { list -> list.map { it.toDomain() } }

    fun favourites(): Flow<List<Song>> = auth.userId
        .flatMapLatest { dao.favourites(it) }.map { list -> list.map { it.toDomain() } }

    fun mostPlayed(): Flow<List<Song>> = auth.userId
        .flatMapLatest { dao.mostPlayed(it) }.map { list -> list.map { it.toDomain() } }

    fun recentlyPlayed(): Flow<List<Song>> = auth.userId
        .flatMapLatest { dao.recentlyPlayed(it) }.map { list -> list.map { it.toDomain() } }

    fun offlinePlayable(): Flow<List<Song>> = auth.userId
        .flatMapLatest { dao.offlinePlayable(it) }.map { list -> list.map { it.toDomain() } }

    suspend fun find(songId: String): Song? =
        dao.findById(songId, auth.userId.value)?.toDomain()

    suspend fun toggleFavourite(song: Song) {
        if (LocalId.isLocal(song.id)) return
        val userId = auth.userId.value
        val existing = dao.findById(song.id, userId)
        val nowLiked = existing?.likedAt == null
        val likedAt = if (nowLiked) System.currentTimeMillis() else null

        if (existing == null) {
            dao.upsert(SongEntity.from(song.copy(userId = userId, likedAt = likedAt)))
        } else {
            dao.setLikedAt(song.id, userId, likedAt)
        }

        outbox.enqueue(userId, OutboxOp.SET_LIKED, buildJsonObject {
            put("songId", song.id)
            put("userId", userId)
            if (likedAt == null) put("likedAt", JsonNull) else put("likedAt", likedAt)
            put("title", song.title)
            put("artistsText", song.artistsText)
            put("durationText", song.durationText)
            put("thumbnailUrl", song.thumbnailUrl)
            put("channelId", song.channelId)
        })
        sync.requestSync()
    }

    suspend fun recordPlayTime(song: Song, deltaMs: Long) {
        if (deltaMs <= 0) return

        if (LocalId.isLocal(song.id)) return
        val userId = auth.userId.value
        val now = System.currentTimeMillis()

        if (dao.findById(song.id, userId) == null) {
            dao.upsert(SongEntity.from(song.copy(userId = userId)))
        }
        dao.addPlayTime(song.id, userId, deltaMs, now)

        outbox.enqueue(userId, OutboxOp.INCREMENT_PLAY_TIME, buildJsonObject {
            put("songId", song.id)
            put("userId", userId)
            put("incrementMs", deltaMs)
            put("title", song.title)
            put("artistsText", song.artistsText)
            put("durationText", song.durationText)
            put("thumbnailUrl", song.thumbnailUrl)
            put("channelId", song.channelId)
        })
        sync.requestSync()
    }

    suspend fun markCacheState(songId: String, state: CacheState) =
        dao.setCacheStateForAllUsers(songId, state)

    suspend fun upsert(song: Song) {
        val userId = auth.userId.value
        val incoming = SongEntity.from(song.copy(userId = userId))
        val existing = dao.findById(song.id, userId)
        dao.upsert(
            if (existing == null) incoming else existing.copy(
                title = incoming.title,
                artistsText = incoming.artistsText ?: existing.artistsText,
                durationText = incoming.durationText ?: existing.durationText,
                thumbnailUrl = incoming.thumbnailUrl ?: existing.thumbnailUrl,
                channelId = incoming.channelId ?: existing.channelId,
            )
        )
    }

    suspend fun upsertAll(songs: List<Song>) {
        val userId = auth.userId.value
        dao.upsertAll(songs.map { SongEntity.from(it.copy(userId = userId)) })
    }
}
