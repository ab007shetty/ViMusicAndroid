package com.abshetty.vimusic.core.media

class PlaybackStatsTracker(
    private val flushThresholdMs: Long = DEFAULT_FLUSH_THRESHOLD_MS,
) {
    var pendingMs: Long = 0
        private set

    fun onTick(elapsedMs: Long, isPlaying: Boolean) {
        if (!isPlaying || elapsedMs <= 0) return
        pendingMs += elapsedMs
    }

    fun flushIfDue(): Long? = if (pendingMs >= flushThresholdMs) flushNow() else null

    fun flushNow(): Long? {
        if (pendingMs <= 0) return null
        return pendingMs.also { pendingMs = 0 }
    }

    fun reset() { pendingMs = 0 }

    companion object { const val DEFAULT_FLUSH_THRESHOLD_MS = 20_000L }
}
