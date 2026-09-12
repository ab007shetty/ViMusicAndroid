package com.abshetty.vimusic.core.data.repository

import com.abshetty.vimusic.core.data.auth.AuthRepository
import com.abshetty.vimusic.core.data.sync.OutboxWriter
import com.abshetty.vimusic.core.data.sync.SyncScheduler
import com.abshetty.vimusic.core.database.dao.SearchHistoryDao
import com.abshetty.vimusic.core.database.entity.OutboxOp
import com.abshetty.vimusic.core.database.entity.SearchHistoryEntity
import com.abshetty.vimusic.core.datastore.SettingsStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SearchHistoryRepository @Inject constructor(
    private val dao: SearchHistoryDao,
    private val settings: SettingsStore,
    private val auth: AuthRepository,
    private val outbox: OutboxWriter,
    private val sync: SyncScheduler,
) {
    fun recent(limit: Int = 20): Flow<List<String>> =
        combine(
            auth.userId.flatMapLatest { dao.recent(it, limit) },
            settings.pauseSearchHistory,
        ) { entries, paused ->
            if (paused) emptyList() else entries.map { it.query }
        }

    val count: Flow<Int> = auth.userId
        .flatMapLatest { dao.recent(it, Int.MAX_VALUE) }
        .map { it.size }

    suspend fun record(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        if (settings.pauseSearchHistory.first()) return

        val userId = auth.userId.value
        val timestamp = System.currentTimeMillis()
        dao.record(SearchHistoryEntity(query = trimmed, userId = userId, timestamp = timestamp))

        outbox.enqueue(userId, OutboxOp.RECORD_SEARCH, buildJsonObject {
            put("query", trimmed)
            put("userId", userId)
            put("timestamp", timestamp)
        })
        sync.requestSync()
    }

    suspend fun remove(query: String) {
        val userId = auth.userId.value
        dao.remove(query, userId)
        outbox.enqueue(userId, OutboxOp.REMOVE_SEARCH, buildJsonObject {
            put("query", query)
            put("userId", userId)
        })
        sync.requestSync()
    }

    suspend fun clear() {
        val userId = auth.userId.value
        dao.clear(userId)
        outbox.enqueue(userId, OutboxOp.CLEAR_SEARCH_HISTORY, buildJsonObject {
            put("userId", userId)
        })
        sync.requestSync()
    }
}
