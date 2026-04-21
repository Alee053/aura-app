package com.programovil.aura.notification.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.programovil.aura.notification.data.NotificationPreferences
import com.programovil.aura.notification.presentation.worker.DailySummaryWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificationViewModel(
    private val notificationPreferences: NotificationPreferences,
    private val workManager: WorkManager
) : ViewModel() {

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled

    private val _hour = MutableStateFlow(8)
    val hour: StateFlow<Int> = _hour

    private val _minute = MutableStateFlow(0)
    val minute: StateFlow<Int> = _minute

    init {
        viewModelScope.launch {
            notificationPreferences.dailySummaryEnabled.collect { _isEnabled.value = it }
        }
        viewModelScope.launch {
            notificationPreferences.notificationHour.collect { _hour.value = it }
        }
        viewModelScope.launch {
            notificationPreferences.notificationMinute.collect { _minute.value = it }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setDailySummaryEnabled(enabled)
            if (enabled) {
                scheduleDailySummary()
            } else {
                cancelDailySummary()
            }
        }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            notificationPreferences.setNotificationTime(hour, minute)
            if (_isEnabled.value) {
                scheduleDailySummary()
            }
        }
    }

    fun scheduleDailySummary() {
        viewModelScope.launch {
            val currentHour = _hour.value
            val currentMinute = _minute.value

            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, currentHour)
                set(Calendar.MINUTE, currentMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val initialDelay = target.timeInMillis - now.timeInMillis

            val workRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                DailySummaryWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_RECREATE,
                workRequest
            )
        }
    }

    private fun cancelDailySummary() {
        workManager.cancelUniqueWork(DailySummaryWorker.WORK_NAME)
    }
}