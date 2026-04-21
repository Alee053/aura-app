package com.programovil.aura.notification.presentation.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.programovil.aura.notification.NotificationHelper

class DueDateNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val todoTitle = inputData.getString(KEY_TODO_TITLE) ?: return Result.failure()
        NotificationHelper.showDueDateNotification(applicationContext, todoTitle)
        return Result.success()
    }

    companion object {
        const val KEY_TODO_TITLE = "todo_title"
        const val WORK_NAME_PREFIX = "due_date_reminder_"
    }
}