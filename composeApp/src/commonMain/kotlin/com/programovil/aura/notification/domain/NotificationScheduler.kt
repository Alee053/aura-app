package com.programovil.aura.notification.domain

interface NotificationScheduler {
    fun scheduleDailySummary(hour: Int, minute: Int)
    fun cancelDailySummary()
    fun testNotification()
    fun scheduleHabitSync()
}
