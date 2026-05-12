package com.programovil.aura.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.home.data.ThemeRepository
import com.programovil.aura.notification.data.NotificationPreferences
import com.programovil.aura.notification.domain.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.PURPLE,
    val notificationsEnabled: Boolean = false,
    val notificationHour: Int = 8,
    val notificationMinute: Int = 0
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
            notificationPreferences.notificationHour.collect { hour ->
                _uiState.update { it.copy(notificationHour = hour) }
                if (_uiState.value.notificationsEnabled) {
                    notificationScheduler.scheduleDailySummary(hour, _uiState.value.notificationMinute)
                }
            }
        }
        viewModelScope.launch {
            notificationPreferences.notificationMinute.collect { minute ->
                _uiState.update { it.copy(notificationMinute = minute) }
                if (_uiState.value.notificationsEnabled) {
                    notificationScheduler.scheduleDailySummary(_uiState.value.notificationHour, minute)
                }
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

    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            notificationPreferences.setNotificationTime(hour, minute)
        }
    }

    fun testNotification() {
        notificationScheduler.testNotification()
    }
}
