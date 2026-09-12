package com.abshetty.vimusic.core.data.sync

import android.util.Log
import com.abshetty.vimusic.core.data.auth.AuthRepository
import com.abshetty.vimusic.core.database.dao.PlaylistDao
import com.abshetty.vimusic.core.database.dao.SearchHistoryDao
import com.abshetty.vimusic.core.database.dao.SongDao
import com.abshetty.vimusic.core.database.entity.PlaylistEntity
import com.abshetty.vimusic.core.database.entity.SearchHistoryEntity
import com.abshetty.vimusic.core.database.entity.SongPlaylistMapEntity
import com.abshetty.vimusic.core.model.LocalId
import com.abshetty.vimusic.core.model.UserId
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRealtime @Inject constructor(
    private val supabase: SupabaseClient,
    private val auth: AuthRepository,
    private val puller: LibraryPuller,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val searchHistoryDao: SearchHistoryDao,
    private val status: SyncStatus,
    private val scope: CoroutineScope,
) {
    private var channel: RealtimeChannel? = null
    private var job: Job? = null

    init {
        scope.launch {
            auth.userId.collect { userId -> resubscribe(userId) }
        }
    }

    private suspend fun resubscribe(userId: String) {
        job?.cancel()
        channel?.let { runCatching { supabase.realtime.removeChannel(it) } }
        channel = null

        if (UserId.isGuest(userId)) return

        val newChannel = supabase.channel("vimusic-" + userId)

        fun changes(table: String) =
            newChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                this.table = table
                filter("user_id", FilterOperator.EQ, userId)
            }

        val songs = changes("song").onEach { applySong(it, userId) }
        val playlists = changes("playlist").onEach { applyPlaylist(it, userId) }
        val maps = changes("song_playlist_map").onEach { applyMapping(it, userId) }
        val searches = changes("search_history").onEach { applySearch(it, userId) }

        job = scope.launch {
            runCatching { merge(songs, playlists, maps, searches).collect() }
                .onFailure { Log.w(TAG, "realtime stream ended: " + it.message) }
        }

        channel = newChannel
        runCatching { newChannel.subscribe(blockUntilSubscribed = true) }
            .onFailure { Log.w(TAG, "could not subscribe: " + it.message) }

        scope.launch {
            newChannel.status.collect { Log.i(TAG, "channel " + it) }
        }
    }

    private suspend fun applySong(action: PostgresAction, userId: String) = with(puller) {
        when (action) {
            is PostgresAction.Delete -> action.oldRecord.str("id")?.let {
                songDao.deleteRow(it, userId)
            }

            is PostgresAction.Insert, is PostgresAction.Update -> {
                val row = action.recordOrNull() ?: return@with
                val remote = row.toSongEntity(userId) ?: return@with

                if (LocalId.isLocal(remote.id)) return@with

                val local = songDao.findById(remote.id, userId)
                songDao.upsert(
                    if (local == null) remote else remote.copy(

                        totalPlayTimeMs = maxOf(local.totalPlayTimeMs, remote.totalPlayTimeMs),

                        cacheState = local.cacheState,
                        downloadState = local.downloadState,
                        dirty = false,
                    )
                )
            }

            else -> Unit
        }
    }

    private suspend fun applyPlaylist(action: PostgresAction, userId: String) = with(puller) {
        when (action) {
            is PostgresAction.Delete -> action.oldRecord.long("id")?.let { remoteId ->
                playlistDao.findByRemoteId(remoteId, userId)?.let {
                    playlistDao.deleteMappingsFor(it.id)
                    playlistDao.deleteRow(it.id)
                }
            }

            is PostgresAction.Insert, is PostgresAction.Update -> {
                val row = action.recordOrNull() ?: return@with
                val remoteId = row.long("id") ?: return@with
                val name = row.str("name") ?: return@with
                val existing = playlistDao.findByRemoteId(remoteId, userId)
                playlistDao.upsert(
                    PlaylistEntity(
                        id = existing?.id ?: 0,
                        userId = userId,
                        name = name,
                        browseId = row.str("browseId"),
                        coverUrl = row.str("coverUrl"),
                        remoteId = remoteId,
                        dirty = false,
                        deletedLocally = existing?.deletedLocally ?: false,
                    )
                )
            }

            else -> Unit
        }
    }

    private suspend fun applyMapping(action: PostgresAction, userId: String) = with(puller) {
        suspend fun localPlaylistId(row: JsonObject): Long? =
            row.long("playlist_id")?.let { playlistDao.findByRemoteId(it, userId)?.id }

        when (action) {
            is PostgresAction.Delete -> {
                val row = action.oldRecord
                val songId = row.str("song_id") ?: return@with
                val playlistId = localPlaylistId(row) ?: return@with
                playlistDao.removeMapping(songId, playlistId, userId)
            }

            is PostgresAction.Insert, is PostgresAction.Update -> {
                val row = action.recordOrNull() ?: return@with
                val songId = row.str("song_id") ?: return@with
                val playlistId = localPlaylistId(row) ?: return@with
                playlistDao.addMapping(
                    SongPlaylistMapEntity(
                        playlistId = playlistId,
                        songId = songId,
                        userId = userId,
                        position = row.long("position")?.toInt() ?: 0,
                    )
                )
            }

            else -> Unit
        }
    }

    private suspend fun applySearch(action: PostgresAction, userId: String) = with(puller) {
        when (action) {
            is PostgresAction.Delete -> action.oldRecord.str("query")?.let {
                searchHistoryDao.remove(it, userId)
            }

            is PostgresAction.Insert, is PostgresAction.Update -> {
                val row = action.recordOrNull() ?: return@with
                val query = row.str("query") ?: return@with
                searchHistoryDao.record(
                    SearchHistoryEntity(
                        query = query,
                        userId = userId,
                        timestamp = row.long("timestamp") ?: 0L,
                    )
                )
            }

            else -> Unit
        }
    }

    private fun PostgresAction.recordOrNull(): JsonObject? = when (this) {
        is PostgresAction.Insert -> record
        is PostgresAction.Update -> record
        else -> null
    }

    private companion object { const val TAG = "ViMusicRealtime" }
}
