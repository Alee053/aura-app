package com.programovil.aura.habit.di

import com.programovil.aura.habit.data.local.getHabitDatabase
import com.programovil.aura.habit.data.local.getHabitDatabaseBuilder
import com.programovil.aura.habit.data.repository.HabitRepositoryImpl
import com.programovil.aura.habit.domain.repository.HabitRepository
import com.programovil.aura.habit.domain.usecase.AddHabitUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitHistoryUseCase
import com.programovil.aura.habit.domain.usecase.GetHabitsGroupedByDayUseCase
import com.programovil.aura.habit.domain.usecase.ToggleHabitCompletionUseCase
import com.programovil.aura.habit.presentation.viewmodel.HabitViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val habitModule = module {
    // Data layer
    single { getHabitDatabaseBuilder() }
    single { getHabitDatabase(get()) }
    single { HabitRepositoryImpl(get()) } bind HabitRepository::class

    // Domain layer - use cases
    factoryOf(::AddHabitUseCase)
    factoryOf(::GetHabitsGroupedByDayUseCase)
    factoryOf(::ToggleHabitCompletionUseCase)
    factoryOf(::GetHabitHistoryUseCase)

    // Presentation layer
    viewModelOf(::HabitViewModel)
}
