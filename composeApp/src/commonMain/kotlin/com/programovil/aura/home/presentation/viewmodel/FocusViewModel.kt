package com.programovil.aura.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FocusUiState(
    val timeRemainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val sessionsToday: Int = 0,
    val selectedDurationMinutes: Int = 25
)

class FocusViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    fun selectDuration(minutes: Int) {
        _uiState.value = _uiState.value.copy(
            selectedDurationMinutes = minutes,
            timeRemainingSeconds = minutes * 60,
            isRunning = false
        )
    }

    fun toggleTimer() {
        _uiState.value = _uiState.value.copy(isRunning = !_uiState.value.isRunning)
    }

    fun resetTimer() {
        _uiState.value = _uiState.value.copy(
            isRunning = false,
            timeRemainingSeconds = _uiState.value.selectedDurationMinutes * 60
        )
    }
}