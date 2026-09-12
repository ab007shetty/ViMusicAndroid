package com.abshetty.vimusic.core.data.sync

import com.abshetty.vimusic.core.database.dao.PlaylistDao
import com.abshetty.vimusic.core.database.dao.SearchHistoryDao
import com.abshetty.vimusic.core.database.dao.SongDao
import com.abshetty.vimusic.core.database.entity.PlaylistEntity
import com.abshetty.vimusic.core.database.entity.SearchHistoryEntity
import com.abshetty.vimusic.core.database.entity.SongEntity
import com.abshetty.vimusic.core.database.entity.SongPlaylistMapEntity
import com.abshetty.vimusic.core.model.LocalId
import com.abshetty.vimusic.core.model.UserId
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryPuller @Inject constructor(
    private val supabase: SupabaseClient,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val searchHistoryDao: SearchHistoryDao,
) {
    private val curator = "ab007shetty@gmail.com"

    suspend fun pull(userId: String): Result<Int> = runCatching {
        val source = if (UserId.isGuest(userId)) curator else userId

        val songs = fetchAll("song", source, "id")

        val localSongs = songDao.snapshot(userId).associateBy { it.id }
        val changedSongs = songs.mapNotNull { row ->
            val remote = row.toSongEntity(userId) ?: return@mapNotNull null

            if (LocalId.isLocal(remote.id)) return@mapNotNull null

            val local = localSongs[remote.id]
            val merged = if (local == null) remote else ConflictResolver.resolve(local, remote)
            merged.takeIf { it != local }
        }
        if (changedSongs.isNotEmpty()) songDao.upsertAll(changedSongs)

        val playlists = fetchAll("playlist", source, "id")

        playlists.forEach { row ->
            val remoteId = row.long("id") ?: return@forEach
            val name = row.str("name") ?: return@forEach

            val existing = playlistDao.findByRemoteId(remoteId, userId)
            val wanted = PlaylistEntity(
                id = existing?.id ?: 0,
                userId = userId,
                name = name,
                browseId = row.str("browseId"),
                coverUrl = row.str("coverUrl"),
                remoteId = remoteId,
                dirty = false,
                deletedLocally = existing?.deletedLocally ?: false,
            )
            if (wanted != existing) playlistDao.upsert(wanted)
        }

        val maps = fetchAll("song_playlist_map", source, "song_id", "playlist_id")

        val localMaps = playlistDao.mappingSnapshot(userId)
            .associateBy { it.songId to it.playlistId }

        maps.forEach { row ->
            val songId = row.str("song_id") ?: return@forEach
            val remotePlaylistId = row.long("playlist_id") ?: return@forEach

            val localPlaylist = playlistDao.findByRemoteId(remotePlaylistId, userId)
                ?: return@forEach
            val wanted = SongPlaylistMapEntity(
                songId = songId,
                playlistId = localPlaylist.id,
                userId = userId,
                position = row.long("position")?.toInt() ?: 1,
                dirty = false,
            )
            if (wanted != localMaps[songId to localPlaylist.id]) {
                playlistDao.addMapping(wanted)
            }
        }

        if (!UserId.isGuest(userId)) {
            val searches = fetchAll("search_history", userId, "query")
            searchHistoryDao.replaceAll(
                userId,
                searches.mapNotNull { row ->
                    val query = row.str("query") ?: return@mapNotNull null
                    SearchHistoryEntity(
                        query = query,
                        userId = userId,
                        timestamp = row.long("timestamp") ?: 0L,
                    )
                },
            )
        }

        songs.size + playlists.size
    }

    private suspend fun fetchAll(
        table: String,
        userId: String,
        vararg orderBy: String,
    ): List<JsonObject> {
        val all = mutableListOf<JsonObject>()
        var from = 0L
        while (all.size < MAX_ROWS) {
            val page = supabase.from(table).select {
                filter { eq("user_id", userId) }
                orderBy.forEach { order(it, Order.ASCENDING) }
                range(from, from + PAGE_SIZE - 1)
            }.decodeList<JsonObject>()

            if (page.isEmpty()) break
            all += page
            from += page.size
        }
        return all
    }

    internal fun JsonObject.toSongEntity(userId: String): SongEntity? {
        val id = str("id") ?: return null
        return SongEntity(
            id = id,
            userId = userId,
            title = str("title") ?: "",
            artistsText = str("artistsText"),
            durationText = str("durationText"),
            thumbnailUrl = str("thumbnailUrl"),
            channelId = str("channelId"),
            likedAt = long("likedAt"),
            totalPlayTimeMs = long("totalPlayTimeMs") ?: 0,
            lastPlayedAt = long("lastPlayedAt"),
        )
    }

    internal fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)
            ?.let { runCatching { it.content }.getOrNull() }
            ?.takeIf { it.isNotEmpty() && it != "null" }

    internal fun JsonObject.long(key: String): Long? = str(key)?.toLongOrNull()

    private companion object {
        const val PAGE_SIZE = 1000L

        const val MAX_ROWS = 200_000
    }
}
