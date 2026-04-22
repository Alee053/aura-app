package com.programovil.aura.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.programovil.aura.R

object NotificationHelper {

    const val CHANNEL_DAILY_SUMMARY = "daily_summary"
    const val CHANNEL_DUE_DATE_REMINDER = "due_date_reminder"
    const val CHANNEL_SYNC = "sync_channel_v2"

    fun createNotificationChannels(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val dailySummaryChannel = NotificationChannel(
            CHANNEL_DAILY_SUMMARY,
            "Daily Summary",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily summary of your incomplete tasks"
        }

        val dueDateChannel = NotificationChannel(
            CHANNEL_DUE_DATE_REMINDER,
            "Due Date Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for tasks due today"
        }

        val syncChannel = NotificationChannel(
            CHANNEL_SYNC,
            "Sync Status",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Synchronization status notifications"
        }

        notificationManager.createNotificationChannels(listOf(dailySummaryChannel, dueDateChannel, syncChannel))
    }

    private fun createPendingIntent(context: Context): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showDailySummaryNotification(context: Context, incompleteCount: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val body = if (incompleteCount > 0) {
            "You have $incompleteCount incomplete task${if (incompleteCount > 1) "s" else ""}"
        } else {
            "All tasks complete! Great job!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Daily Summary")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent(context))
            .build()

        notificationManager.notify(NOTIFICATION_ID_DAILY_SUMMARY, notification)
    }

    fun showDueDateNotification(context: Context, todoTitle: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL_DUE_DATE_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Task Due Today")
            .setContentText(todoTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent(context))
            .build()

        notificationManager.notify(NOTIFICATION_ID_DUE_DATE, notification)
    }

    fun showSyncSummaryNotification(context: Context, syncedCount: Int, failedCount: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val body = when {
            failedCount > 0 -> "Synced $syncedCount items, $failedCount failed"
            syncedCount > 0 -> "Successfully synced $syncedCount item${if (syncedCount > 1) "s" else ""}"
            else -> "No items to sync"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Sync Complete")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent(context))
            .build()

        notificationManager.notify(NOTIFICATION_ID_SYNC, notification)
    }

    private const val NOTIFICATION_ID_DAILY_SUMMARY = 1001
    private const val NOTIFICATION_ID_DUE_DATE = 1002
    private const val NOTIFICATION_ID_SYNC = 1003
}