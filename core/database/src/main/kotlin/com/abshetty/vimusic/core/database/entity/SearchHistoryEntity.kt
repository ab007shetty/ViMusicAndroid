package com.abshetty.vimusic.core.database.entity

import androidx.room.Entity

@Entity(tableName = "search_history", primaryKeys = ["query", "userId"])
data class SearchHistoryEntity(
    val query: String,
    val userId: String,
    val timestamp: Long,
)
