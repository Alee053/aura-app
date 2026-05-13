package com.programovil.aura

import androidx.compose.ui.window.ComposeUIViewController
import com.programovil.aura.di.getModules
import com.programovil.aura.shared.StubRemoteConfigService
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController {
    startKoin {
        modules(getModules(StubRemoteConfigService()))
    }
    App()
}