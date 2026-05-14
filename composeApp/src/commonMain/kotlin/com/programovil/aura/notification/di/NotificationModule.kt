package com.programovil.aura.notification.di

import com.programovil.aura.notification.data.NotificationPreferences
import com.programovil.aura.notification.domain.NotificationScheduler
import org.koin.dsl.module

expect fun createNotificationScheduler(): NotificationScheduler

val notificationModule = module {

    single { NotificationPreferences(get()) }
    single { createNotificationScheduler() }
}
