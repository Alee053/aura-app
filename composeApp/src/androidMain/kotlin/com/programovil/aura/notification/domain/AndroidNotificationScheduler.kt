package com.programovil.aura.notification.domain

import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.programovil.aura.experiments.domain.usecase.GetNotificationVariantUseCase
import com.programovil.aura.notification.presentation.worker.DailySummaryWorker
import com.programovil.aura.notification.presentation.worker.PomodoroCompletionWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AndroidNotificationScheduler(
    private val workManager: WorkManager
) : NotificationScheduler, KoinComponent {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

        scope.launch {
            val variant = runCatching {
                val getNotificationVariantUseCase: GetNotificationVariantUseCase = get()
                getNotificationVariantUseCase()
            }.getOrNull()

            if (variant != null && variant.timesPerDay >= 2) {
                val eveningHour = (hour + 12) % 24
                val eveningTarget = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, eveningHour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (before(Calendar.getInstance())) add(Calendar.DAY_OF_MONTH, 1)
                }
                val eveningDelay = eveningTarget.timeInMillis - Calendar.getInstance().timeInMillis
                val eveningRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(
                    1, TimeUnit.DAYS
                ).setInitialDelay(eveningDelay, TimeUnit.MILLISECONDS).build()
                workManager.enqueueUniquePeriodicWork(
                    "daily_summary_work_evening",
                    ExistingPeriodicWorkPolicy.KEEP,
                    eveningRequest
                )
                Log.d("AndroidSched", "Scheduled evening daily summary (Premium)")
            } else {
                workManager.cancelUniqueWork("daily_summary_work_evening")
            }
        }
    }

    override fun cancelDailySummary() {
        workManager.cancelUniqueWork(DailySummaryWorker.WORK_NAME)
        workManager.cancelUniqueWork("daily_summary_work_evening")
    }

    override fun schedulePomodoroCompletion(delayMillis: Long) {
        val workRequest = OneTimeWorkRequestBuilder<PomodoroCompletionWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            PomodoroCompletionWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    override fun cancelPomodoroCompletion() {
        workManager.cancelUniqueWork(PomodoroCompletionWorker.WORK_NAME)
    }

    override fun showPomodoroCompletionNow() {
        val request = OneTimeWorkRequestBuilder<PomodoroCompletionWorker>()
            .setInputData(PomodoroCompletionWorker.forceNotifyInputData())
            .build()
        workManager.enqueue(request)
    }

    override fun testNotification() {
        val testRequest = OneTimeWorkRequestBuilder<DailySummaryWorker>().build()
        workManager.enqueue(testRequest)
    }
}
