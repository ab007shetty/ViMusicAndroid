package com.abshetty.vimusic.core.data.auth

import com.abshetty.vimusic.core.model.UserId

object SessionIdentity {
    fun from(email: String?): String = UserId.of(email)
    fun isAuthenticated(userId: String): Boolean = !UserId.isGuest(userId)
}
