package com.abshetty.vimusic.core.data.sync

import com.abshetty.vimusic.core.data.ConnectivityObserver
import com.abshetty.vimusic.core.data.auth.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import com.abshetty.vimusic.core.model.UserId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class LibrarySyncCoordinator @Inject constructor(
    private val puller: LibraryPuller,
    private val syncer: OutboxSyncer,
    private val auth: AuthRepository,
    private val status: SyncStatus,
    connectivity: ConnectivityObserver,
    private val scope: CoroutineScope,
) {
    init {
        scope.launch {
            combine(auth.userId, connectivity.isOnline) { userId, online -> userId to online }
                .distinctUntilChanged()
                .collect { (userId, online) ->

                    if (online) syncNow()
                }
        }
    }

    private val requests = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            for (request in requests) {
                runCatching { runSync() }
            }
        }
    }

    fun syncNow() {
        requests.trySend(Unit)
    }

    private suspend fun runSync() {
        if (!auth.isResolved.value) {
            auth.isResolved.first { it }
        }

        val userId = auth.userId.value
        if (UserId.isGuest(userId)) {
            status.track { puller.pull(userId).getOrThrow() }
        } else {
            syncer.drain()
        }
    }
}
