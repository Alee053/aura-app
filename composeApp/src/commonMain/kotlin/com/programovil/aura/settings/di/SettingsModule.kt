package com.programovil.aura.settings.di

import com.programovil.aura.settings.data.ThemeRepositoryImpl
import com.programovil.aura.settings.domain.repository.ThemeRepository
import com.programovil.aura.settings.presentation.viewmodel.SettingsViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val settingsModule = module {
    singleOf(::ThemeRepositoryImpl) bind ThemeRepository::class
    viewModelOf(::SettingsViewModel)
}
