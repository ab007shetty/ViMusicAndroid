package com.abshetty.vimusic.core.data.repository

import com.abshetty.vimusic.core.data.auth.AuthRepository
import com.abshetty.vimusic.core.database.dao.LyricsDao
import com.abshetty.vimusic.core.database.entity.LyricsEntity
import com.abshetty.vimusic.core.model.UserId
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

data class LyricLine(val timeMs: Long, val text: String)

data class Lyrics(val plain: String?, val synced: List<LyricLine>) {
    val hasSynced: Boolean get() = synced.isNotEmpty()
    val isEmpty: Boolean get() = plain.isNullOrBlank() && synced.isEmpty()
}

@Singleton
class LyricsRepository @Inject constructor(
    private val dao: LyricsDao,
    private val supabase: SupabaseClient,
    private val auth: AuthRepository,
    private val http: HttpClient,
) {
    suspend fun lyricsFor(
        songId: String,
        title: String,
        artist: String?,
        durationSeconds: Int?,
    ): Lyrics {
        val userId = auth.userId.value

        dao.find(songId, userId)?.let { cached ->
            return Lyrics(cached.fixed, parseLrc(cached.synced))
        }

        val fetched = search(title, artist, durationSeconds)

        val plain = fetched?.str("plainLyrics")
        val syncedRaw = fetched?.str("syncedLyrics")

        if (plain == null && syncedRaw == null) return Lyrics(null, emptyList())

        dao.upsert(
            LyricsEntity(songId = songId, userId = userId, fixed = plain, synced = syncedRaw)
        )

        if (!UserId.isGuest(userId)) {
            runCatching {
                supabase.from("lyrics").upsert(
                    buildJsonObject {
                        put("song_id", songId)
                        put("user_id", userId)
                        put("fixed", plain)
                        put("synced", syncedRaw)
                    }
                ) { onConflict = "song_id,user_id" }
            }
        }

        return Lyrics(plain, parseLrc(syncedRaw))
    }

    internal fun parseLrc(raw: String?): List<LyricLine> {
        if (raw.isNullOrBlank()) return emptyList()
        val pattern = Regex("""^\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]\s?(.*)$""")

        return raw.lineSequence().mapNotNull { line ->
            val m = pattern.find(line.trim()) ?: return@mapNotNull null
            val (min, sec, frac, text) = m.destructured
            val fracMs = when (frac.length) {
                0 -> 0L
                1 -> frac.toLong() * 100
                2 -> frac.toLong() * 10
                else -> frac.toLong()
            }
            LyricLine(
                timeMs = min.toLong() * 60_000 + sec.toLong() * 1000 + fracMs,
                text = text.trim(),
            )
        }.filter { it.text.isNotEmpty() }.sortedBy { it.timeMs }.toList()
    }

    private fun String.cleanedForSearch(): String = this
        .replace(Regex("""\((?i)[^)]*(official|video|audio|lyric|hd|4k|remaster)[^)]*\)"""), "")
        .replace(Regex("""\[(?i)[^]]*(official|video|audio|lyric|hd|4k|remaster)[^]]*]"""), "")
        .substringBefore('|')
        .trim()

    private suspend fun search(
        title: String,
        artist: String?,
        durationSeconds: Int?,
    ): JsonObject? {
        val track = title.cleanedForSearch()
        val performer = artist.orEmpty().asArtistName()

        val split = Regex("""^(.{2,60}?)\s+-\s+(.+)$""").find(track)
        val splitArtist = split?.groupValues?.get(1)?.trim()
        val splitTrack = split?.groupValues?.get(2)?.trim()

        if (performer.isNotBlank()) {
            exact(track, performer)?.let { return it }
        }
        if (splitArtist != null && splitTrack != null) {
            exact(splitTrack, splitArtist)?.let { return it }
        }

        query(track, durationSeconds)?.let { return it }
        if (performer.isNotBlank()) {
            query(track + " " + performer, durationSeconds)?.let { return it }
        }
        return null
    }

    private suspend fun exact(track: String, performer: String): JsonObject? =
        runCatching {
            http.get("https://lrclib.net/api/get") {
                parameter("track_name", track)
                parameter("artist_name", performer)
            }.body<JsonObject>()
        }.getOrNull()?.takeIf { it.hasLyrics() }

    private suspend fun query(q: String, duration: Int?): JsonObject? =
        runCatching {
            http.get("https://lrclib.net/api/search") {
                parameter("q", q)
            }.body<JsonArray>()
        }.getOrNull()?.bestMatch(duration)

    private fun JsonObject.hasLyrics() =
        str("plainLyrics") != null || str("syncedLyrics") != null

    private fun String.asArtistName(): String = this
        .removeSuffix(" - Topic")
        .replace(Regex("""VEVO$""", RegexOption.IGNORE_CASE), "")
        .cleanedForSearch()

    private fun JsonArray.bestMatch(durationSeconds: Int?): JsonObject? {
        val candidates = filterIsInstance<JsonObject>().filter { it.hasLyrics() }
        if (durationSeconds == null) return candidates.firstOrNull()

        return candidates
            .mapNotNull { candidate ->
                val length = (candidate["duration"] as? JsonPrimitive)
                    ?.content?.toDoubleOrNull()?.toInt() ?: return@mapNotNull null
                candidate to kotlin.math.abs(length - durationSeconds)
            }
            .filter { it.second <= MAX_DURATION_DRIFT_SECONDS }
            .minByOrNull { it.second }
            ?.first
            ?: candidates.firstOrNull()
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)
            ?.let { runCatching { it.content }.getOrNull() }
            ?.takeIf { it.isNotBlank() && it != "null" }

    private companion object {
        const val MAX_DURATION_DRIFT_SECONDS = 30
    }
}
