package com.programovil.aura

import android.app.Application
import com.programovil.aura.data.remote.FirebaseConfig
import com.programovil.aura.di.getModules
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
