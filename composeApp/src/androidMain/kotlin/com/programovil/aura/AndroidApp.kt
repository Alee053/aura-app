package com.programovil.aura

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.programovil.aura.notification.NotificationHelper
import com.programovil.aura.shared.FirebaseConfig
import com.programovil.aura.shared.FirebaseRemoteConfigService
import com.programovil.aura.shared.AppVisibilityTracker
import com.programovil.aura.di.getModules
import org.koin.android.ext.koin.androidLogger
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AndroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseConfig.initialize(this)
        FirebaseConfig.messaging.subscribeToTopic("test-notifications")
            .addOnCompleteListener { }
        NotificationHelper.createNotificationChannels(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    AppVisibilityTracker.markForeground()
                }

                override fun onStop(owner: LifecycleOwner) {
                    AppVisibilityTracker.markBackground()
                }
            }
        )

        val remoteConfigService = FirebaseRemoteConfigService(this)

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@AndroidApp)
            modules(getModules(remoteConfigService))
        }

        com.programovil.aura.experiments.presentation.worker.ExperimentsHeartbeatWorker.schedule(this)
    }
}
