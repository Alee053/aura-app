package com.programovil.aura.notification.di

import com.programovil.aura.notification.domain.IosNotificationScheduler
import com.programovil.aura.notification.domain.NotificationScheduler

actual fun createNotificationScheduler(): NotificationScheduler {
    return IosNotificationScheduler()
}
