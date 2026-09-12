package com.abshetty.vimusic.core.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncPhase { IDLE, SYNCING, FAILED }

data class SyncState(
    val phase: SyncPhase = SyncPhase.IDLE,
    val lastSyncedAtMs: Long? = null,
    val message: String? = null,
) {
    val isSyncing: Boolean get() = phase == SyncPhase.SYNCING
}

@Singleton
class SyncStatus @Inject constructor() {
    private val _state = MutableStateFlow(SyncState())
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private var active = 0
    private val lock = Any()

    suspend fun <T> track(block: suspend () -> T): Result<T> {
        begin()
        val result = runCatching { block() }
        end(result.exceptionOrNull()?.message)
        return result
    }

    private fun begin() = synchronized(lock) {
        active++
        _state.update { it.copy(phase = SyncPhase.SYNCING, message = null) }
    }

    private fun end(error: String?) = synchronized(lock) {
        active = (active - 1).coerceAtLeast(0)
        _state.update { current ->
            when {
                active > 0 -> current
                error != null -> current.copy(phase = SyncPhase.FAILED, message = error)
                else -> current.copy(
                    phase = SyncPhase.IDLE,
                    lastSyncedAtMs = System.currentTimeMillis(),
                    message = null,
                )
            }
        }
    }
}
