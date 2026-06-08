package com.programovil.aura.pomodoro.di

import com.programovil.aura.pomodoro.presentation.PomodoroViewModel
import org.koin.compose.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val pomodoroModule = module {
    viewModelOf(::PomodoroViewModel)
}
