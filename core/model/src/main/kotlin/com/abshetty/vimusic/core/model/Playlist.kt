package com.abshetty.vimusic.core.model

data class Playlist(
    val id: Long,
    val userId: String,
    val name: String,
    val browseId: String? = null,
    val songCount: Int = 0,
    val coverUrl: String? = null,
)
