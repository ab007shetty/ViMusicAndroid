package com.abshetty.vimusic.core.model

object UserId {
    const val GUEST: String = ""

    private val SENTINELS = setOf("null", "undefined", "guest")

    fun of(email: String?): String {
        val normalized = email?.trim()?.lowercase() ?: return GUEST
        if (normalized.isEmpty() || normalized in SENTINELS) return GUEST
        return normalized
    }

    fun isGuest(userId: String): Boolean = userId == GUEST
}
