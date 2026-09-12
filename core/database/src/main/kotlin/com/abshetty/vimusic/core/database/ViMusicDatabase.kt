package com.abshetty.vimusic.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.abshetty.vimusic.core.database.dao.LyricsDao
import com.abshetty.vimusic.core.database.dao.OutboxDao
import com.abshetty.vimusic.core.database.dao.PlaylistDao
import com.abshetty.vimusic.core.database.dao.SearchHistoryDao
import com.abshetty.vimusic.core.database.dao.SongDao
import com.abshetty.vimusic.core.database.entity.LyricsEntity
import com.abshetty.vimusic.core.database.entity.OutboxEntity
import com.abshetty.vimusic.core.database.entity.OutboxOp
import com.abshetty.vimusic.core.database.entity.PlaylistEntity
import com.abshetty.vimusic.core.database.entity.SearchHistoryEntity
import com.abshetty.vimusic.core.database.entity.SongEntity
import com.abshetty.vimusic.core.database.entity.SongPlaylistMapEntity
import com.abshetty.vimusic.core.model.CacheState
import com.abshetty.vimusic.core.model.DownloadState

class Converters {
    @TypeConverter fun cacheToString(v: CacheState): String = v.name
    @TypeConverter fun stringToCache(v: String): CacheState = CacheState.valueOf(v)
    @TypeConverter fun downloadToString(v: DownloadState): String = v.name
    @TypeConverter fun stringToDownload(v: String): DownloadState = DownloadState.valueOf(v)
    @TypeConverter fun opToString(v: OutboxOp): String = v.name
    @TypeConverter fun stringToOp(v: String): OutboxOp = OutboxOp.valueOf(v)
}

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        SongPlaylistMapEntity::class,
        OutboxEntity::class,
        SearchHistoryEntity::class,
        LyricsEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ViMusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun outboxDao(): OutboxDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun lyricsDao(): LyricsDao
}
