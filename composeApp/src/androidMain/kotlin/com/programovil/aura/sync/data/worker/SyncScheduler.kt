package com.programovil.aura.sync.data.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SyncScheduler {
    private const val SYNC_INTERVAL_MINUTES = 15L

    fun schedulePeriodicSync(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val syncRequest = PeriodicWorkRequestBuilder<SyncQueueWorker>(
            SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            SyncQueueWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    fun triggerImmediateSync(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val syncRequest = androidx.work.OneTimeWorkRequestBuilder<SyncQueueWorker>().build()
        workManager.enqueue(syncRequest)
    }
}