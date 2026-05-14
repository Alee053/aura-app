package com.programovil.aura.notification.domain

import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

class IosNotificationScheduler : NotificationScheduler {

    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    override fun scheduleDailySummary(hour: Int, minute: Int) {
        val content = UNMutableNotificationContent().apply {
            setTitle("Daily Summary")
            setBody("Check your incomplete tasks for today")
            setSound(UNNotificationSound.defaultSound)
        }

        val dateComponents = NSDateComponents().apply {
            setHour(hour.toLong())
            setMinute(minute.toLong())
        }

        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents,
            repeats = true
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            DAILY_SUMMARY_ID,
            content = content,
            trigger = trigger
        )

        notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(DAILY_SUMMARY_ID))
        notificationCenter.addNotificationRequest(request) { error ->
            if (error != null) {
                println("Error scheduling daily summary: $error")
            }
        }
    }

    override fun cancelDailySummary() {
        notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(DAILY_SUMMARY_ID))
    }

    override fun testNotification() {
        val content = UNMutableNotificationContent().apply {
            setTitle("Test Notification")
            setBody("Your notifications are working!")
            setSound(UNNotificationSound.defaultSound)
        }

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = 1.0,
            repeats = false
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            TEST_NOTIFICATION_ID,
            content = content,
            trigger = trigger
        )

        notificationCenter.addNotificationRequest(request) { error ->
            if (error != null) {
                println("Error scheduling test notification: $error")
            }
        }
    }

    companion object {
        private const val DAILY_SUMMARY_ID = "daily_summary"
        private const val TEST_NOTIFICATION_ID = "test_notification"
    }
}
