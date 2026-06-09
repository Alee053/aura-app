package com.programovil.aura.pomodoro.domain

import kotlinx.coroutines.flow.Flow

data class PomodoroTimerState(
    val timeLeftSeconds: Int = PomodoroDefaults.POMODORO_MINUTES * 60,
    val initialTimeSeconds: Int = PomodoroDefaults.POMODORO_MINUTES * 60,
    val isRunning: Boolean = false,
    val mode: PomodoroMode = PomodoroMode.POMODORO,
    val sessionsCompleted: Int = 0,
    val selectedOption: Int = PomodoroDefaults.POMODORO_MINUTES,
    val endsAtEpochMillis: Long? = null,
    val showCompletionMessage: Boolean = false,
    val completedMode: PomodoroMode? = null
)

interface PomodoroStateRepository {
    val state: Flow<PomodoroTimerState>

    suspend fun saveState(state: PomodoroTimerState)
}
