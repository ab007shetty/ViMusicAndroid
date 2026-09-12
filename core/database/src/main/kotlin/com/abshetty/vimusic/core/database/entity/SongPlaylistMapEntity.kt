package com.abshetty.vimusic.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "song_playlist_map",
    primaryKeys = ["songId", "playlistId"],
    indices = [Index("playlistId"), Index("userId")],
    foreignKeys = [ForeignKey(
        entity = PlaylistEntity::class,
        parentColumns = ["id"],
        childColumns = ["playlistId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class SongPlaylistMapEntity(
    val songId: String,
    val playlistId: Long,
    val userId: String,
    val position: Int,
    val dirty: Boolean = false,
    val deletedLocally: Boolean = false,
)
