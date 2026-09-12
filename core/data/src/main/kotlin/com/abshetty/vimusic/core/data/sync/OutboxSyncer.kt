package com.abshetty.vimusic.core.data.sync

import com.abshetty.vimusic.core.database.dao.OutboxDao
import com.abshetty.vimusic.core.database.dao.PlaylistDao
import com.abshetty.vimusic.core.database.entity.OutboxEntity
import com.abshetty.vimusic.core.database.entity.OutboxOp
import com.abshetty.vimusic.core.data.auth.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

enum class DrainOutcome { DRAINED, RETRY_LATER }

@Singleton
class OutboxSyncer @Inject constructor(
    private val outbox: OutboxDao,
    private val playlists: PlaylistDao,
    private val supabase: SupabaseClient,
    private val puller: LibraryPuller,
    private val auth: AuthRepository,
    private val status: SyncStatus,
) {
    private val mutex = Mutex()

    suspend fun drain(): DrainOutcome =
        mutex.withLock { status.track { drainLocked() }.getOrDefault(DrainOutcome.RETRY_LATER) }

    private suspend fun drainLocked(): DrainOutcome {
        while (true) {
            val batch = outbox.pending(BATCH_SIZE)
            if (batch.isEmpty()) {
                puller.pull(auth.userId.value)
                return DrainOutcome.DRAINED
            }

            for (entry in batch) {
                val payload = runCatching {
                    Json.parseToJsonElement(entry.payload) as JsonObject
                }.getOrNull()

                if (payload == null) {
                    outbox.delete(entry.id)
                    continue
                }

                val outcome = runCatching { apply(entry, payload) }
                when {
                    outcome.isSuccess -> outbox.delete(entry.id)

                    outcome.isPermanentFailure() -> {
                        outbox.recordFailure(entry.id, outcome.errorText())
                        outbox.delete(entry.id)
                    }

                    else -> {
                        outbox.recordFailure(entry.id, outcome.errorText())
                        return DrainOutcome.RETRY_LATER
                    }
                }
            }
        }
        @Suppress("UNREACHABLE_CODE")
        return DrainOutcome.DRAINED
    }

    private suspend fun apply(entry: OutboxEntity, p: JsonObject) {
        when (entry.op) {
            OutboxOp.SET_LIKED, OutboxOp.UPSERT_SONG ->
                supabase.from("song").upsert(songRow(p)) { onConflict = "id,user_id" }

            OutboxOp.INCREMENT_PLAY_TIME -> {
                supabase.postgrest.rpc("increment_play_time", buildJsonObject {
                    put("p_song_id", p.str("songId"))
                    put("p_user_id", p.str("userId"))
                    put("p_ms", p.long("incrementMs") ?: 0L)
                    put("p_title", p.str("title") ?: "")
                    put("p_artists", p.str("artistsText"))
                    put("p_duration", p.str("durationText"))
                    put("p_thumbnail", p.str("thumbnailUrl"))
                    put("p_channel_id", p.str("channelId"))
                })
            }

            OutboxOp.CREATE_PLAYLIST -> {
                val created = supabase.from("playlist").insert(
                    buildJsonObject {
                        put("user_id", p.str("userId"))
                        put("name", p.str("name"))
                    }
                ) { select() }.decodeSingle<JsonObject>()

                val remoteId = created["id"]?.jsonPrimitive?.content?.toLongOrNull()
                val localId = p.long("localId")
                if (remoteId != null && localId != null) {
                    playlists.attachRemoteId(localId, remoteId)
                }
            }

            OutboxOp.RENAME_PLAYLIST ->
                supabase.from("playlist").update(
                    buildJsonObject { put("name", p.str("name")) }
                ) {
                    filter {
                        eq("id", remoteIdFor(p))
                        eq("user_id", p.str("userId") ?: "")
                    }
                }

            OutboxOp.SET_PLAYLIST_COVER ->
                supabase.from("playlist").update(

                    buildJsonObject {
                        put(
                            "coverUrl",
                            p.str("coverUrl")?.let { JsonPrimitive(it) } ?: JsonNull,
                        )
                    }
                ) {
                    filter {
                        eq("id", remoteIdFor(p))
                        eq("user_id", p.str("userId") ?: "")
                    }
                }

            OutboxOp.DELETE_PLAYLIST ->
                supabase.from("playlist").delete {
                    filter {
                        eq("id", remoteIdFor(p))
                        eq("user_id", p.str("userId") ?: "")
                    }
                }

            OutboxOp.ADD_SONG_TO_PLAYLIST -> {
                supabase.from("song").upsert(songRow(p)) { onConflict = "id,user_id" }
                supabase.from("song_playlist_map").upsert(
                    buildJsonObject {
                        put("song_id", p.str("songId"))
                        put("playlist_id", remoteIdFor(p))
                        put("user_id", p.str("userId"))
                        put("position", p.long("position") ?: 1L)
                    }
                ) { onConflict = "song_id,playlist_id" }
            }

            OutboxOp.REMOVE_SONG_FROM_PLAYLIST ->
                supabase.from("song_playlist_map").delete {
                    filter {
                        eq("song_id", p.str("songId") ?: "")
                        eq("playlist_id", remoteIdFor(p))
                        eq("user_id", p.str("userId") ?: "")
                    }
                }

            OutboxOp.REORDER_PLAYLIST -> {
                val playlistId = remoteIdFor(p)
                val userId = p.str("userId") ?: ""
                val songIds = (p["songIds"] as? kotlinx.serialization.json.JsonArray)
                    ?.mapNotNull { (it as? JsonPrimitive)?.content }
                    .orEmpty()

                if (songIds.isNotEmpty()) {
                    supabase.from("song_playlist_map").upsert(
                        songIds.mapIndexed { index, songId ->
                            buildJsonObject {
                                put("song_id", songId)
                                put("playlist_id", playlistId)
                                put("user_id", userId)
                                put("position", index)
                            }
                        }
                    ) { onConflict = "song_id,playlist_id" }
                }
            }

            OutboxOp.RECORD_SEARCH ->
                supabase.from("search_history").upsert(
                    buildJsonObject {
                        put("query", p.str("query"))
                        put("user_id", p.str("userId"))
                        put("timestamp", p.long("timestamp") ?: 0L)
                    }
                ) { onConflict = "query,user_id" }

            OutboxOp.REMOVE_SEARCH ->
                supabase.from("search_history").delete {
                    filter {
                        eq("query", p.str("query") ?: "")
                        eq("user_id", p.str("userId") ?: "")
                    }
                }

            OutboxOp.CLEAR_SEARCH_HISTORY ->
                supabase.from("search_history").delete {
                    filter { eq("user_id", p.str("userId") ?: "") }
                }

            OutboxOp.SET_PLAYBACK_STATE ->
                supabase.from("playback_state").upsert(
                    buildJsonObject {
                        put("user_id", p.str("userId"))
                        put("song_id", p.str("songId"))
                        put("position_ms", p.long("positionMs") ?: 0L)
                        put("updated_at", p.long("updatedAt") ?: 0L)
                    }
                ) { onConflict = "user_id" }
        }
    }

    private suspend fun remoteIdFor(p: JsonObject): Long {
        val localId = p.long("localId") ?: return -1L
        val userId = p.str("userId") ?: ""
        return playlists.findById(localId, userId)?.remoteId ?: localId
    }

    private fun Result<*>.errorText(): String =
        exceptionOrNull()?.message?.take(200) ?: "unknown"

    private fun Result<*>.isPermanentFailure(): Boolean {
        val message = exceptionOrNull()?.message.orEmpty()
        if (message.contains("429")) return false
        return Regex("""\b4\d\d\b""").containsMatchIn(message)
    }

    private companion object { const val BATCH_SIZE = 50 }
}

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.let { runCatching { it.content }.getOrNull() }
        ?.takeIf { it != "null" }

private fun JsonObject.long(key: String): Long? = str(key)?.toLongOrNull()

internal fun songRow(p: JsonObject) = buildJsonObject {
    put("id", p.str("songId"))
    put("user_id", p.str("userId"))
    put("title", p.str("title") ?: "")
    put("artistsText", p.str("artistsText"))
    put("durationText", p.str("durationText"))
    put("thumbnailUrl", p.str("thumbnailUrl"))
    put("channelId", p.str("channelId"))
    if ("likedAt" in p) put("likedAt", p.long("likedAt"))
}
