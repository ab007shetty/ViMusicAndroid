package com.abshetty.vimusic.core.media.embedded

sealed interface Recovery {
    data class Reload(val attempt: Int) : Recovery
    data class GiveUp(val message: String) : Recovery
}

const val MAX_RELOAD_ATTEMPTS = 3

fun recoveryFor(attemptsSoFar: Int): Recovery =
    if (attemptsSoFar >= MAX_RELOAD_ATTEMPTS) {
        Recovery.GiveUp("Could not reach YouTube. Check your connection and try again.")
    } else {
        Recovery.Reload(attemptsSoFar + 1)
    }
