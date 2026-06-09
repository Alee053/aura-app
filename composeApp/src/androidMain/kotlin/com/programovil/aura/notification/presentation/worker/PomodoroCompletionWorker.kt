package com.programovil.aura.notification.presentation.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programovil.aura.notification.NotificationHelper

class PomodoroCompletionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        NotificationHelper.showPomodoroCompletionNotification(applicationContext)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "pomodoro_completion_work"
    }
}
