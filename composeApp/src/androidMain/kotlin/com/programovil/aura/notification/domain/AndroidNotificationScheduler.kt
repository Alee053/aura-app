package com.programovil.aura.notification.domain

import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.programovil.aura.notification.presentation.worker.DailySummaryWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AndroidNotificationScheduler(
    private val workManager: WorkManager
) : NotificationScheduler {

    override fun scheduleDailySummary(hour: Int, minute: Int) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        Log.d("AndroidSched", "Scheduling daily summary. Target: ${target.time}, Initial delay: ${initialDelay}ms")

        val workRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DailySummaryWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    override fun cancelDailySummary() {
        workManager.cancelUniqueWork(DailySummaryWorker.WORK_NAME)
    }

    override fun testNotification() {
        val testRequest = OneTimeWorkRequestBuilder<DailySummaryWorker>().build()
        workManager.enqueue(testRequest)
    }
}
