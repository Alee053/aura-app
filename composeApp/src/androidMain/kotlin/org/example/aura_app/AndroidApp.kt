package org.example.aura_app

import android.app.Application
import org.example.aura_app.data.remote.FirebaseConfig
import org.example.aura_app.di.getModules
import org.koin.android.ext.koin.androidLogger
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AndroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseConfig.initialize(this)
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@AndroidApp)
            modules(getModules())
        }
    }
}
