package com.programovil.aura.notification.domain

interface NotificationScheduler {
    fun scheduleDailySummary(hour: Int, minute: Int)
    fun cancelDailySummary()
    fun schedulePomodoroCompletion(delayMillis: Long)
    fun cancelPomodoroCompletion()
    fun showPomodoroCompletionNow()
    fun testNotification()
}
