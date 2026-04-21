package com.programovil.aura.notification.di

import androidx.work.WorkManager
import com.programovil.aura.notification.NotificationHelper
import com.programovil.aura.notification.data.NotificationPreferences
import com.programovil.aura.notification.presentation.viewmodel.NotificationViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val notificationModule = module {
    single { NotificationPreferences(androidContext()) }
    single { WorkManager.getInstance(androidContext()) }
    single { NotificationHelper }
    viewModel { NotificationViewModel(get(), get()) }
}