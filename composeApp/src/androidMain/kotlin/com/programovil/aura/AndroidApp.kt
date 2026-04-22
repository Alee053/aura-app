package com.programovil.aura

import android.app.Application
import android.util.Log
import com.programovil.aura.notification.NotificationHelper
import com.programovil.aura.shared.FirebaseConfig
import com.programovil.aura.shared.FirebaseRemoteConfigService
import com.programovil.aura.di.getModules
import org.koin.android.ext.koin.androidLogger
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

private const val TAG = "AndroidApp"

class AndroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: starting...")
        FirebaseConfig.initialize(this)
        FirebaseConfig.messaging.subscribeToTopic("test-notifications")
            .addOnCompleteListener { }
        NotificationHelper.createNotificationChannels(this)

        Log.d(TAG, "Creating FirebaseRemoteConfigService...")
        val remoteConfigService = FirebaseRemoteConfigService(this)
        Log.d(TAG, "FirebaseRemoteConfigService created, starting Koin...")

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@AndroidApp)
            modules(getModules(remoteConfigService))
        }
        Log.d(TAG, "Koin started, app ready")
    }
}
