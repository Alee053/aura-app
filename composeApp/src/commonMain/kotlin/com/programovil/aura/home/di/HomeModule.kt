package com.programovil.aura.home.di

import com.programovil.aura.home.data.ThemeRepository
import com.programovil.aura.home.data.ThemeRepositoryImpl
import com.programovil.aura.home.presentation.viewmodel.FocusViewModel
import com.programovil.aura.home.presentation.viewmodel.HomeViewModel
import com.programovil.aura.home.presentation.viewmodel.ProgressViewModel
import com.programovil.aura.home.presentation.viewmodel.SettingsViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val homeModule = module {
    singleOf(::ThemeRepositoryImpl) bind ThemeRepository::class
    viewModelOf(::HomeViewModel)
    viewModelOf(::FocusViewModel)
    viewModelOf(::ProgressViewModel)
    viewModelOf(::SettingsViewModel)
}