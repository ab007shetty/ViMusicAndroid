package com.abshetty.vimusic.core.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.abshetty.vimusic.core.data.auth.AuthRepository
import com.abshetty.vimusic.core.data.sync.SyncScheduler
import com.abshetty.vimusic.core.database.dao.PlaylistDao
import com.abshetty.vimusic.core.database.dao.SearchHistoryDao
import com.abshetty.vimusic.core.database.dao.SongDao
import com.abshetty.vimusic.core.database.entity.OutboxOp
import com.abshetty.vimusic.core.data.sync.OutboxWriter
import com.abshetty.vimusic.core.database.entity.PlaylistEntity
import com.abshetty.vimusic.core.database.entity.SongEntity
import com.abshetty.vimusic.core.database.entity.SongPlaylistMapEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseBackup @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val auth: AuthRepository,
    private val searchHistory: SearchHistoryDao,
    private val outbox: OutboxWriter,
    private val sync: SyncScheduler,
) {
    suspend fun export(output: OutputStream): Result<Int> = runCatching {
        val userId = auth.userId.value
        val temp = File(context.cacheDir, "export-" + System.currentTimeMillis() + ".db")

        val db = SQLiteDatabase.openOrCreateDatabase(temp, null)
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS android_metadata (locale TEXT)")
            db.execSQL("DELETE FROM android_metadata")
            db.execSQL("INSERT INTO android_metadata VALUES ('en_US')")
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS Song (
                    id TEXT NOT NULL, title TEXT NOT NULL, artistsText TEXT,
                    durationText TEXT, thumbnailUrl TEXT, likedAt INTEGER,
                    totalPlayTimeMs INTEGER NOT NULL DEFAULT 0, channelId TEXT,
                    lastPlayedAt INTEGER, PRIMARY KEY(id))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS Playlist (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL, browseId TEXT)"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS SongPlaylistMap (
                    songId TEXT NOT NULL, playlistId INTEGER NOT NULL,
                    position INTEGER NOT NULL, PRIMARY KEY(songId, playlistId))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS Lyrics (
                    songId TEXT NOT NULL, fixed TEXT, synced TEXT, PRIMARY KEY(songId))"""
            )
            db.execSQL(
                "CREATE VIEW SortedSongPlaylistMap AS SELECT * FROM SongPlaylistMap ORDER BY position"
            )
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")

            db.execSQL(
                """CREATE TABLE IF NOT EXISTS Artist (
                    id TEXT NOT NULL, name TEXT, thumbnailUrl TEXT,
                    timestamp INTEGER, bookmarkedAt INTEGER, PRIMARY KEY(id))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS SongArtistMap (
                    songId TEXT NOT NULL, artistId TEXT NOT NULL,
                    PRIMARY KEY(songId, artistId))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS Album (
                    id TEXT NOT NULL, title TEXT, thumbnailUrl TEXT, year TEXT,
                    authorsText TEXT, shareUrl TEXT, timestamp INTEGER,
                    bookmarkedAt INTEGER, PRIMARY KEY(id))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS SongAlbumMap (
                    songId TEXT NOT NULL, albumId TEXT NOT NULL, position INTEGER,
                    PRIMARY KEY(songId, albumId))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS SearchQuery (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    query TEXT NOT NULL)"""
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_SearchQuery_query ON SearchQuery(query)"
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS QueuedMediaItem (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    mediaItem BLOB NOT NULL, position INTEGER)"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS Format (
                    songId TEXT NOT NULL, itag INTEGER, mimeType TEXT, bitrate INTEGER,
                    contentLength INTEGER, lastModified INTEGER, loudnessDb REAL,
                    PRIMARY KEY(songId))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS Event (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    songId TEXT NOT NULL, timestamp INTEGER NOT NULL,
                    playTime INTEGER NOT NULL)"""
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_Event_songId ON Event(songId)")
            db.execSQL(
                "CREATE VIEW IF NOT EXISTS SortedSongPlaylistMap AS " +
                    "SELECT * FROM SongPlaylistMap ORDER BY position"
            )

            var rows = 0

            val songs = songDao.allSongs(userId).first()

            songs.forEach { s ->
                db.execSQL(
                    "INSERT OR IGNORE INTO Song VALUES (?,?,?,?,?,?,?,?,?)",
                    arrayOf<Any?>(
                        s.id, s.title, s.artistsText, s.durationText, s.thumbnailUrl,
                        s.likedAt, s.totalPlayTimeMs, s.channelId, s.lastPlayedAt,
                    )
                )
                rows++
            }

            searchHistory.recent(userId, Int.MAX_VALUE).first().forEach { entry ->
                db.execSQL(
                    "INSERT OR IGNORE INTO SearchQuery (query) VALUES (?)",
                    arrayOf<Any?>(entry.query),
                )
                rows++
            }

            songs.filter { it.totalPlayTimeMs > 0 }.forEach { song ->
                db.execSQL(
                    "INSERT INTO Event (songId, timestamp, playTime) VALUES (?,?,?)",
                    arrayOf<Any?>(
                        song.id,
                        song.lastPlayedAt ?: System.currentTimeMillis(),
                        song.totalPlayTimeMs,
                    ),
                )
                rows++
            }

            playlistDao.playlists(userId).first().forEach { p ->
                db.execSQL(
                    "INSERT OR IGNORE INTO Playlist (id, name, browseId) VALUES (?,?,?)",
                    arrayOf<Any?>(p.id, p.name, p.browseId)
                )
                rows++

                playlistDao.songsIn(p.id, userId).first().forEachIndexed { index, song ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO SongPlaylistMap VALUES (?,?,?)",
                        arrayOf<Any?>(song.id, p.id, index + 1)
                    )
                }
            }

            db.close()
            temp.inputStream().use { it.copyTo(output) }
            rows
        } finally {
            if (db.isOpen) db.close()
            temp.delete()
        }
    }

    suspend fun import(input: InputStream): Result<Int> = runCatching {
        val userId = auth.userId.value
        val temp = File(context.cacheDir, "import-" + System.currentTimeMillis() + ".db")
        temp.outputStream().use { input.copyTo(it) }

        val db = SQLiteDatabase.openDatabase(temp.path, null, SQLiteDatabase.OPEN_READONLY)
        var imported = 0
        try {
            db.rawQuery(
                "SELECT id, title, artistsText, durationText, thumbnailUrl, likedAt, " +
                    "totalPlayTimeMs, channelId, lastPlayedAt FROM Song", null
            ).use { c ->
                while (c.moveToNext()) {
                    val song = SongEntity(
                        id = c.getString(0),
                        userId = userId,
                        title = c.getString(1) ?: "",
                        artistsText = c.getStringOrNull(2),
                        durationText = c.getStringOrNull(3),
                        thumbnailUrl = c.getStringOrNull(4),
                        likedAt = c.getLongOrNull(5),
                        totalPlayTimeMs = c.getLongOrNull(6) ?: 0,
                        channelId = c.getStringOrNull(7),
                        lastPlayedAt = c.getLongOrNull(8),
                    )
                    songDao.upsert(song)
                    imported++

                    outbox.enqueue(userId, OutboxOp.UPSERT_SONG, buildJsonObject {
                        put("songId", song.id); put("userId", userId)
                        put("title", song.title); put("artistsText", song.artistsText)
                        put("durationText", song.durationText)
                        put("thumbnailUrl", song.thumbnailUrl)
                        put("channelId", song.channelId)
                        put("likedAt", song.likedAt)
                    })
                }
            }

            val idMap = mutableMapOf<Long, Long>()
            db.rawQuery("SELECT id, name, browseId FROM Playlist", null).use { c ->
                while (c.moveToNext()) {
                    val fileId = c.getLong(0)
                    val name = c.getString(1) ?: "Imported"
                    val existing = playlistDao.findByName(name, userId)

                    if (existing != null) {
                        idMap[fileId] = existing.id
                        continue
                    }

                    val localId = playlistDao.upsert(
                        PlaylistEntity(
                            userId = userId,
                            name = name,
                            browseId = c.getStringOrNull(2),
                            dirty = true,
                        )
                    )
                    idMap[fileId] = localId
                    imported++
                    outbox.enqueue(userId, OutboxOp.CREATE_PLAYLIST, buildJsonObject {
                        put("localId", localId); put("userId", userId)
                        put("name", name)
                    })
                }
            }

            db.rawQuery("SELECT songId, playlistId, position FROM SongPlaylistMap", null).use { c ->
                while (c.moveToNext()) {
                    val localPlaylistId = idMap[c.getLong(1)] ?: continue
                    playlistDao.addMapping(
                        SongPlaylistMapEntity(
                            songId = c.getString(0),
                            playlistId = localPlaylistId,
                            userId = userId,
                            position = c.getInt(2),
                            dirty = true,
                        )
                    )
                    outbox.enqueue(userId, OutboxOp.ADD_SONG_TO_PLAYLIST, buildJsonObject {
                        put("localId", localPlaylistId); put("songId", c.getString(0))
                        put("userId", userId); put("position", c.getInt(2))
                    })
                }
            }

            sync.requestSync()
            imported
        } finally {
            if (db.isOpen) db.close()
            temp.delete()
        }
    }

    private fun android.database.Cursor.getStringOrNull(i: Int): String? =
        if (isNull(i)) null else getString(i)

    private fun android.database.Cursor.getLongOrNull(i: Int): Long? =
        if (isNull(i)) null else getLong(i)
}
