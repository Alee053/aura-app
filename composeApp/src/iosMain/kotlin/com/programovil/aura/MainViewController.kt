package com.programovil.aura

import androidx.compose.ui.window.ComposeUIViewController
import com.programovil.aura.di.getModules
import com.programovil.aura.shared.StubRemoteConfigService
import org.koin.core.context.startKoin

private val koinApp by lazy {
    startKoin {
        modules(getModules(StubRemoteConfigService()))
    }
}

fun MainViewController() = ComposeUIViewController {
    koinApp
    App()
}