package com.programovil.aura.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.notification.data.NotificationPreferences
import com.programovil.aura.notification.domain.NotificationScheduler
import com.programovil.aura.settings.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.PURPLE,
    val notificationsEnabled: Boolean = false,
    val notificationHour: Int = 8,
    val notificationMinute: Int = 0,
    val soundsEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true
)

class SettingsViewModel(
    private val themeRepository: ThemeRepository,
    private val notificationPreferences: NotificationPreferences,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            themeRepository.getThemeMode().collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            notificationPreferences.dailySummaryEnabled.collect { enabled ->
                _uiState.update { it.copy(notificationsEnabled = enabled) }
                if (enabled) {
                    val state = _uiState.value
                    notificationScheduler.scheduleDailySummary(state.notificationHour, state.notificationMinute)
                } else {
                    notificationScheduler.cancelDailySummary()
                }
            }
        }
        viewModelScope.launch {
            notificationPreferences.soundsEnabled.collect { enabled ->
                _uiState.update { it.copy(soundsEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            notificationPreferences.vibrationEnabled.collect { enabled ->
                _uiState.update { it.copy(vibrationEnabled = enabled) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeRepository.setThemeMode(mode)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setDailySummaryEnabled(enabled)
        }
    }

    fun setSoundsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setSoundsEnabled(enabled)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setVibrationEnabled(enabled)
        }
    }
}
