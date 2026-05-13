package com.programovil.aura.home.di

import com.programovil.aura.home.domain.usecase.GetDashboardDataUseCase
import com.programovil.aura.home.presentation.viewmodel.FocusViewModel
import com.programovil.aura.home.presentation.viewmodel.HomeViewModel
import com.programovil.aura.home.presentation.viewmodel.ProgressViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val homeModule = module {
    factoryOf(::GetDashboardDataUseCase)
    viewModelOf(::HomeViewModel)
    viewModelOf(::FocusViewModel)
    viewModelOf(::ProgressViewModel)
}
