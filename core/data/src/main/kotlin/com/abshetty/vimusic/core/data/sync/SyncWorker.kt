package com.abshetty.vimusic.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncer: OutboxSyncer,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (syncer.drain()) {
        DrainOutcome.DRAINED -> Result.success()
        DrainOutcome.RETRY_LATER -> Result.retry()
    }
}
