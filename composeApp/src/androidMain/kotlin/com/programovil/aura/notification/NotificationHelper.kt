package com.programovil.aura.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.programovil.aura.R

object NotificationHelper {

    const val CHANNEL_DAILY_SUMMARY = "daily_summary"
    const val CHANNEL_DUE_DATE_REMINDER = "due_date_reminder"
    const val CHANNEL_HABIT_SYNC = "habit_sync"

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

        val habitSyncChannel = NotificationChannel(
            CHANNEL_HABIT_SYNC,
            "Habit Synchronization",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Confirms when habits are synchronized with the cloud"
        }

        notificationManager.createNotificationChannels(
            listOf(dailySummaryChannel, dueDateChannel, habitSyncChannel)
        )
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

    fun showHabitSyncNotification(context: Context, syncedCount: Int) {
        if (!canPostNotifications(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_HABIT_SYNC)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Habits synchronized")
            .setContentText("Sync complete: $syncedCount habit${if (syncedCount == 1) "" else "s"} saved to Firestore.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent(context))
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_HABIT_SYNC, notification)
    }

    private fun canPostNotifications(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private const val NOTIFICATION_ID_DAILY_SUMMARY = 1001
    private const val NOTIFICATION_ID_DUE_DATE = 1002
    private const val NOTIFICATION_ID_HABIT_SYNC = 1003
}
