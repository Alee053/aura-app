package com.programovil.aura.notification.di

import com.programovil.aura.notification.data.NotificationPreferences
import com.programovil.aura.notification.domain.NotificationScheduler
import com.programovil.aura.notification.presentation.viewmodel.NotificationViewModel
import com.programovil.aura.shared.data.createDataStore
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect fun createNotificationScheduler(): NotificationScheduler

val notificationModule = module {
    single { createDataStore() }
    single { NotificationPreferences(get()) }
    single { createNotificationScheduler() }
    viewModelOf(::NotificationViewModel)
}
