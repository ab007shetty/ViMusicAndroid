package com.abshetty.vimusic.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import com.abshetty.vimusic.core.model.CacheState
import com.abshetty.vimusic.core.model.DownloadState
import com.abshetty.vimusic.core.model.HtmlText
import com.abshetty.vimusic.core.model.Song

@Entity(
    tableName = "song",
    primaryKeys = ["id", "userId"],
    indices = [Index("userId"), Index("likedAt"), Index("lastPlayedAt")],
)
data class SongEntity(
    val id: String,
    val userId: String,
    val title: String,
    val artistsText: String? = null,
    val durationText: String? = null,
    val thumbnailUrl: String? = null,
    val channelId: String? = null,
    val likedAt: Long? = null,
    val totalPlayTimeMs: Long = 0,
    val lastPlayedAt: Long? = null,

    val cacheState: CacheState = CacheState.NONE,
    val downloadState: DownloadState = DownloadState.NONE,
    val dirty: Boolean = false,
) {
    fun toDomain() = Song(
        id = id, userId = userId,
        title = HtmlText.decode(title),
        artistsText = artistsText?.let(HtmlText::decode),
        durationText = durationText, thumbnailUrl = thumbnailUrl, channelId = channelId,
        likedAt = likedAt, totalPlayTimeMs = totalPlayTimeMs, lastPlayedAt = lastPlayedAt,
        cacheState = cacheState, downloadState = downloadState,
    )

    companion object {
        fun from(song: Song) = SongEntity(
            id = song.id, userId = song.userId, title = song.title,
            artistsText = song.artistsText, durationText = song.durationText,
            thumbnailUrl = song.thumbnailUrl, channelId = song.channelId,
            likedAt = song.likedAt, totalPlayTimeMs = song.totalPlayTimeMs,
            lastPlayedAt = song.lastPlayedAt, cacheState = song.cacheState,
            downloadState = song.downloadState,
        )
    }
}
