package com.programovil.aura.home.di

import com.programovil.aura.home.presentation.viewmodel.FocusViewModel
import com.programovil.aura.home.presentation.viewmodel.HomeViewModel
import com.programovil.aura.home.presentation.viewmodel.ProgressViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::FocusViewModel)
    viewModelOf(::ProgressViewModel)
}