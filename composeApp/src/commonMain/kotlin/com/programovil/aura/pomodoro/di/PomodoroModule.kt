package com.programovil.aura.pomodoro.di

import com.programovil.aura.pomodoro.data.PomodoroPreferencesRepository
import com.programovil.aura.pomodoro.domain.PomodoroStateRepository
import com.programovil.aura.pomodoro.domain.SystemTimeProvider
import com.programovil.aura.pomodoro.domain.TimeProvider
import com.programovil.aura.pomodoro.presentation.PomodoroViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val pomodoroModule = module {
    singleOf(::PomodoroPreferencesRepository) bind PomodoroStateRepository::class
    single<TimeProvider> { SystemTimeProvider }
    viewModelOf(::PomodoroViewModel)
}
