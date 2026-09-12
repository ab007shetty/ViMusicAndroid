package com.abshetty.vimusic.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OutboxOp {
    UPSERT_SONG,
    SET_LIKED,
    INCREMENT_PLAY_TIME,
    CREATE_PLAYLIST,
    RENAME_PLAYLIST,
    SET_PLAYLIST_COVER,
    DELETE_PLAYLIST,
    ADD_SONG_TO_PLAYLIST,
    REMOVE_SONG_FROM_PLAYLIST,
    REORDER_PLAYLIST,
    SET_PLAYBACK_STATE,
    RECORD_SEARCH,
    REMOVE_SEARCH,
    CLEAR_SEARCH_HISTORY,
}

@Entity(tableName = "sync_outbox")
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val op: OutboxOp,

    val payload: String,
    val createdAt: Long,
    val attempts: Int = 0,
    val lastError: String? = null,
)
