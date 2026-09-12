package com.abshetty.vimusic.core.data.sync

import com.abshetty.vimusic.core.database.dao.OutboxDao
import com.abshetty.vimusic.core.database.entity.OutboxEntity
import com.abshetty.vimusic.core.database.entity.OutboxOp
import com.abshetty.vimusic.core.model.UserId
import kotlinx.serialization.json.JsonObject

class OutboxWriter constructor(
    private val dao: OutboxDao,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    suspend fun enqueue(userId: String, op: OutboxOp, payload: JsonObject) {
        if (UserId.isGuest(userId)) return

        dao.enqueue(OutboxEntity(op = op, payload = payload.toString(), createdAt = nowMs()))
    }
}
