package com.programovil.aura.notification.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programovil.aura.notification.data.NotificationPreferences
import com.programovil.aura.notification.domain.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val notificationPreferences: NotificationPreferences,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled

    private val _hour = MutableStateFlow(8)
    val hour: StateFlow<Int> = _hour

    private val _minute = MutableStateFlow(0)
    val minute: StateFlow<Int> = _minute

    init {
        viewModelScope.launch {
            notificationPreferences.dailySummaryEnabled.collect { enabled ->
                _isEnabled.value = enabled
                if (enabled) {
                    notificationScheduler.scheduleDailySummary(_hour.value, _minute.value)
                } else {
                    notificationScheduler.cancelDailySummary()
                }
            }
        }
        viewModelScope.launch {
            notificationPreferences.notificationHour.collect { 
                _hour.value = it
                if (_isEnabled.value) {
                    notificationScheduler.scheduleDailySummary(it, _minute.value)
                }
            }
        }
        viewModelScope.launch {
            notificationPreferences.notificationMinute.collect { 
                _minute.value = it
                if (_isEnabled.value) {
                    notificationScheduler.scheduleDailySummary(_hour.value, it)
                }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
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
