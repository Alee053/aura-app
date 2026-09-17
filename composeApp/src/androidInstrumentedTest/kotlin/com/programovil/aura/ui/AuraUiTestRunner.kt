package com.programovil.aura.ui

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** UI fixtures never start AndroidApp, Koin, authentication, workers, or messaging subscriptions. */
class AuraUiTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, Application::class.java.name, context)
}
