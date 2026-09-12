package com.abshetty.vimusic.core.model

object LocalId {
    const val PREFIX = "local:"

    const val USER = "@local"

    fun isLocal(songId: String) = songId.startsWith(PREFIX)

    fun of(uri: String) = PREFIX + uri

    fun uriOf(songId: String): String = songId.removePrefix(PREFIX)
}
