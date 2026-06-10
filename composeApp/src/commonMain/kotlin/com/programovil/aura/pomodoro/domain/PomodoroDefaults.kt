package com.programovil.aura.pomodoro.domain

object PomodoroDefaults {
    const val POMODORO_MINUTES = 25
    const val SHORT_BREAK_MINUTES = 5
    const val LONG_BREAK_MINUTES = 15
    const val SESSIONS_BEFORE_LONG_BREAK = 4
    const val TICK_INTERVAL_MS = 1000L

    val MANUAL_DURATIONS_MINUTES = listOf(5, 25, 10)
}
