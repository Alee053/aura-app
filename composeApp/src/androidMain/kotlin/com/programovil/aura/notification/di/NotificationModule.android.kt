package com.programovil.aura.notification.di

import android.content.Context
import androidx.work.WorkManager
import com.programovil.aura.notification.domain.AndroidNotificationScheduler
import com.programovil.aura.notification.domain.NotificationScheduler
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

actual fun createNotificationScheduler(): NotificationScheduler {
    val context: Context = object : KoinComponent {
        val ctx: Context = get()
    }.ctx
    val workManager = WorkManager.getInstance(context)
    return AndroidNotificationScheduler(workManager)
}
