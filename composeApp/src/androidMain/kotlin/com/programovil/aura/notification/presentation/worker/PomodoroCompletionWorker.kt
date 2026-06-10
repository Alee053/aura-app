package com.programovil.aura.notification.presentation.worker

import android.content.Context
import androidx.work.Data
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programovil.aura.notification.NotificationHelper
import com.programovil.aura.pomodoro.data.PomodoroPreferencesRepository
import com.programovil.aura.pomodoro.domain.PomodoroCompletionHandler
import com.programovil.aura.pomodoro.domain.SystemTimeProvider
import com.programovil.aura.shared.AppVisibilityTracker
import com.programovil.aura.shared.data.createDataStore
import kotlinx.coroutines.flow.first

class PomodoroCompletionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val forceNotify = inputData.getBoolean(KEY_FORCE_NOTIFY, false)
        if (forceNotify) {
            NotificationHelper.showPomodoroCompletionNotification(applicationContext)
            return Result.success()
        }

        val repository = PomodoroPreferencesRepository(createDataStore())
        val state = repository.state.first()
        val now = SystemTimeProvider.currentTimeMillis()
        val shouldNotify = PomodoroCompletionHandler.isExpired(state, now)

        if (shouldNotify) {
            repository.saveState(PomodoroCompletionHandler.advanceToNextSession(state))
        }

        if (shouldNotify && !AppVisibilityTracker.isForeground.value) {
            NotificationHelper.showPomodoroCompletionNotification(applicationContext)
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "pomodoro_completion_work"
        private const val KEY_FORCE_NOTIFY = "key_force_notify"

        fun forceNotifyInputData(): Data {
            return Data.Builder()
                .putBoolean(KEY_FORCE_NOTIFY, true)
                .build()
        }
    }
}
