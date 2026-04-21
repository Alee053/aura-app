package com.programovil.aura.notification.di

import androidx.work.WorkManager
import com.programovil.aura.notification.NotificationHelper
import com.programovil.aura.notification.data.NotificationPreferences
import com.programovil.aura.notification.presentation.viewmodel.NotificationViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val notificationModule = module {
    singleOf(::NotificationPreferences).bind<NotificationPreferences>()
    single { WorkManager.getInstance(androidContext()) }
    singleOf(::NotificationHelper)
    viewModel { NotificationViewModel(get(), get()) }
}