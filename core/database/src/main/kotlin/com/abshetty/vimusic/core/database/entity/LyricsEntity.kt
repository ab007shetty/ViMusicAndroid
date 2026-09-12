package com.abshetty.vimusic.core.database.entity

import androidx.room.Entity

@Entity(tableName = "lyrics", primaryKeys = ["songId", "userId"])
data class LyricsEntity(
    val songId: String,
    val userId: String,
    val fixed: String? = null,
    val synced: String? = null,
)
