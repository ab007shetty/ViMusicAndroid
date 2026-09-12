package com.abshetty.vimusic.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlist", indices = [Index("userId")])
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val name: String,
    val browseId: String? = null,

    val remoteId: Long? = null,

    val coverUrl: String? = null,
    val dirty: Boolean = false,
    val deletedLocally: Boolean = false,
)
