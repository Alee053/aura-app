package com.programovil.aura.habit.di

import com.programovil.aura.habit.domain.repository.HabitRepository
import com.programovil.aura.habit.domain.repository.createHabitRepository
import com.programovil.aura.habit.domain.usecase.AddHabitUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitHistoryUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitsGroupedByDayUseCase
import com.programovil.aura.habit.domain.usecase.ToggleHabitCompletionUseCase
import com.programovil.aura.habit.presentation.viewmodel.HabitViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val habitModule = module {
    // Data layer
    single { createHabitRepository() }

    // Domain layer - use cases
    factoryOf(::AddHabitUseCase)
    factoryOf(::GetHabitsGroupedByDayUseCase)
    factoryOf(::ToggleHabitCompletionUseCase)
    factoryOf(::GetHabitHistoryUseCase)

    // Presentation layer
    viewModelOf(::HabitViewModel)
}
