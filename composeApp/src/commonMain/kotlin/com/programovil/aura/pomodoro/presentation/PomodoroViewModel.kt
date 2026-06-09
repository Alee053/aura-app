package com.programovil.aura.pomodoro.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.notification.domain.NotificationScheduler
import com.programovil.aura.pomodoro.domain.PomodoroDefaults
import com.programovil.aura.pomodoro.domain.PomodoroMode
import com.programovil.aura.pomodoro.domain.PomodoroStateRepository
import com.programovil.aura.pomodoro.domain.PomodoroTimerState
import com.programovil.aura.pomodoro.domain.TimeProvider
import com.programovil.aura.shared.AppVisibilityTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PomodoroUiState(
    val timeLeftSeconds: Int = PomodoroDefaults.POMODORO_MINUTES * 60,
    val initialTimeSeconds: Int = PomodoroDefaults.POMODORO_MINUTES * 60,
    val isRunning: Boolean = false,
    val mode: PomodoroMode = PomodoroMode.POMODORO,
    val sessionsCompleted: Int = 0,
    val selectedOption: Int = PomodoroDefaults.POMODORO_MINUTES,
    val showCompletionMessage: Boolean = false,
    val completedMode: PomodoroMode? = null
) {
    val progress: Float
        get() = if (initialTimeSeconds > 0) timeLeftSeconds.toFloat() / initialTimeSeconds else 0f
}

class PomodoroViewModel(
    private val repository: PomodoroStateRepository,
    private val timeProvider: TimeProvider,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {
    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var persistedState = PomodoroTimerState()
    private var isAdvancingSession = false

    init {
        viewModelScope.launch {
            repository.state.collect { state ->
                persistedState = state
                if (shouldAdvanceCompletedTimer(state)) {
                    advanceToNextSession(state)
                } else {
                    publishState(state)
                    if (state.isRunning) {
                        ensureTicker()
                    } else {
                        stopTicker()
                    }
                }
            }
        }
    }

    fun onTimeOptionSelected(minutes: Int) {
        notificationScheduler.cancelPomodoroCompletion()
        val updatedState = persistedState.copy(
            timeLeftSeconds = minutes * 60,
            initialTimeSeconds = minutes * 60,
            selectedOption = minutes,
            isRunning = false,
            endsAtEpochMillis = null
        )
        saveState(updatedState)
    }

    fun toggleTimer() {
        if (persistedState.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun resetTimer() {
        notificationScheduler.cancelPomodoroCompletion()
        saveState(
            persistedState.copy(
                timeLeftSeconds = persistedState.initialTimeSeconds,
                isRunning = false,
                endsAtEpochMillis = null
            )
        )
    }

    fun skipSession() {
        viewModelScope.launch {
            notificationScheduler.cancelPomodoroCompletion()
            advanceToNextSession(persistedState.copy(isRunning = false, endsAtEpochMillis = null))
        }
    }

    fun dismissCompletionMessage() {
        saveState(
            persistedState.copy(
                showCompletionMessage = false,
                completedMode = null
            )
        )
    }

    private fun startTimer() {
        val remainingSeconds = computeRemainingSeconds(persistedState)
        notificationScheduler.schedulePomodoroCompletion(remainingSeconds * 1000L)
        val updatedState = persistedState.copy(
            timeLeftSeconds = remainingSeconds,
            isRunning = true,
            endsAtEpochMillis = timeProvider.currentTimeMillis() + remainingSeconds * 1000L
        )
        saveState(updatedState)
    }

    private fun pauseTimer() {
        val remainingSeconds = computeRemainingSeconds(persistedState)
        notificationScheduler.cancelPomodoroCompletion()
        saveState(
            persistedState.copy(
                timeLeftSeconds = remainingSeconds,
                isRunning = false,
                endsAtEpochMillis = null
            )
        )
    }

    private fun ensureTicker() {
        if (timerJob?.isActive == true) {
            return
        }

        timerJob = viewModelScope.launch {
            while (persistedState.isRunning) {
                val remainingSeconds = computeRemainingSeconds(persistedState)
                if (remainingSeconds <= 0) {
                    publishState(persistedState.copy(timeLeftSeconds = 0))
                    advanceToNextSession(persistedState)
                    break
                }
                publishState(persistedState.copy(timeLeftSeconds = remainingSeconds))
                delay(PomodoroDefaults.TICK_INTERVAL_MS)
            }
        }
    }

    private fun stopTicker() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun publishState(state: PomodoroTimerState) {
        val remainingSeconds = computeRemainingSeconds(state)
        _uiState.update {
            it.copy(
                timeLeftSeconds = remainingSeconds,
                initialTimeSeconds = state.initialTimeSeconds,
                isRunning = state.isRunning,
                mode = state.mode,
                sessionsCompleted = state.sessionsCompleted,
                selectedOption = state.selectedOption,
                showCompletionMessage = state.showCompletionMessage,
                completedMode = state.completedMode
            )
        }
    }

    private fun computeRemainingSeconds(state: PomodoroTimerState): Int {
        if (!state.isRunning) {
            return state.timeLeftSeconds.coerceAtLeast(0)
        }

        val endsAt = state.endsAtEpochMillis ?: return state.timeLeftSeconds.coerceAtLeast(0)
        val millisLeft = endsAt - timeProvider.currentTimeMillis()
        return ((millisLeft + 999L) / 1000L).coerceAtLeast(0L).toInt()
    }

    private fun shouldAdvanceCompletedTimer(state: PomodoroTimerState): Boolean {
        return state.isRunning && computeRemainingSeconds(state) <= 0 && !isAdvancingSession
    }

    private suspend fun advanceToNextSession(state: PomodoroTimerState) {
        if (isAdvancingSession) {
            return
        }

        isAdvancingSession = true
        notificationScheduler.cancelPomodoroCompletion()
        stopTicker()
        try {
            val completedMode = state.mode
            val isForeground = AppVisibilityTracker.isForeground.value
            val newSessionsCompleted: Int
            val nextMode: PomodoroMode
            val nextMinutes: Int

            if (completedMode == PomodoroMode.POMODORO) {
                newSessionsCompleted = state.sessionsCompleted + 1
                if (newSessionsCompleted % PomodoroDefaults.SESSIONS_BEFORE_LONG_BREAK == 0) {
                    nextMode = PomodoroMode.LONG_BREAK
                    nextMinutes = PomodoroDefaults.LONG_BREAK_MINUTES
                } else {
                    nextMode = PomodoroMode.SHORT_BREAK
                    nextMinutes = PomodoroDefaults.SHORT_BREAK_MINUTES
                }
            } else {
                newSessionsCompleted = state.sessionsCompleted
                nextMode = PomodoroMode.POMODORO
                nextMinutes = PomodoroDefaults.POMODORO_MINUTES
            }

            repository.saveState(
                state.copy(
                    mode = nextMode,
                    sessionsCompleted = newSessionsCompleted,
                    timeLeftSeconds = nextMinutes * 60,
                    initialTimeSeconds = nextMinutes * 60,
                    selectedOption = nextMinutes,
                    isRunning = false,
                    endsAtEpochMillis = null,
                    showCompletionMessage = true,
                    completedMode = completedMode
                )
            )
            if (!isForeground) {
                notificationScheduler.showPomodoroCompletionNow()
            }
        } finally {
            isAdvancingSession = false
        }
    }

    private fun saveState(state: PomodoroTimerState) {
        stopTicker()
        viewModelScope.launch {
            repository.saveState(state)
        }
    }
}
