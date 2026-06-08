package com.programovil.aura.pomodoro.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.pomodoro.domain.PomodoroDefaults
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PomodoroMode {
    POMODORO, SHORT_BREAK, LONG_BREAK
}

data class PomodoroUiState(
    val timeLeftSeconds: Int = PomodoroDefaults.POMODORO_MINUTES * 60,
    val initialTimeSeconds: Int = PomodoroDefaults.POMODORO_MINUTES * 60,
    val isRunning: Boolean = false,
    val mode: PomodoroMode = PomodoroMode.POMODORO,
    val sessionsCompleted: Int = 0,
    val selectedOption: Int = PomodoroDefaults.POMODORO_MINUTES
) {
    val progress: Float
        get() = if (initialTimeSeconds > 0) timeLeftSeconds.toFloat() / initialTimeSeconds else 0f
}

class PomodoroViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun onTimeOptionSelected(minutes: Int) {
        stopTimer()
        _uiState.update {
            it.copy(
                timeLeftSeconds = minutes * 60,
                initialTimeSeconds = minutes * 60,
                selectedOption = minutes,
                isRunning = false
            )
        }
    }

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            stopTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        _uiState.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeLeftSeconds > 0) {
                delay(PomodoroDefaults.TICK_INTERVAL_MS)
                _uiState.update { it.copy(timeLeftSeconds = it.timeLeftSeconds - 1) }
            }
            onTimerFinished()
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isRunning = false) }
    }

    fun resetTimer() {
        stopTimer()
        _uiState.update {
            it.copy(
                timeLeftSeconds = it.initialTimeSeconds,
                isRunning = false
            )
        }
    }

    fun skipSession() {
        stopTimer()
        onTimerFinished()
    }

    private fun onTimerFinished() {
        _uiState.update { state ->
            val nextMode: PomodoroMode
            val nextMinutes: Int
            var newSessionsCompleted = state.sessionsCompleted

            if (state.mode == PomodoroMode.POMODORO) {
                newSessionsCompleted++
                if (newSessionsCompleted % PomodoroDefaults.SESSIONS_BEFORE_LONG_BREAK == 0) {
                    nextMode = PomodoroMode.LONG_BREAK
                    nextMinutes = PomodoroDefaults.LONG_BREAK_MINUTES
                } else {
                    nextMode = PomodoroMode.SHORT_BREAK
                    nextMinutes = PomodoroDefaults.SHORT_BREAK_MINUTES
                }
            } else {
                nextMode = PomodoroMode.POMODORO
                nextMinutes = PomodoroDefaults.POMODORO_MINUTES
            }

            state.copy(
                mode = nextMode,
                sessionsCompleted = newSessionsCompleted,
                timeLeftSeconds = nextMinutes * 60,
                initialTimeSeconds = nextMinutes * 60,
                selectedOption = nextMinutes,
                isRunning = false
            )
        }
    }
}
