package com.programovil.aura.notification.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotificationPreferences(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val DAILY_SUMMARY_ENABLED = booleanPreferencesKey("daily_summary_enabled")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        val SOUNDS_ENABLED = booleanPreferencesKey("sounds_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }

    val dailySummaryEnabled: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[Keys.DAILY_SUMMARY_ENABLED] ?: false }

    val notificationHour: Flow<Int> = dataStore.data
        .map { prefs -> prefs[Keys.NOTIFICATION_HOUR] ?: 8 }

    val notificationMinute: Flow<Int> = dataStore.data
        .map { prefs -> prefs[Keys.NOTIFICATION_MINUTE] ?: 0 }

    val soundsEnabled: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[Keys.SOUNDS_ENABLED] ?: false }

    val vibrationEnabled: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[Keys.VIBRATION_ENABLED] ?: true }

    suspend fun setDailySummaryEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DAILY_SUMMARY_ENABLED] = enabled }
    }

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_HOUR] = hour
            prefs[Keys.NOTIFICATION_MINUTE] = minute
        }
    }

    suspend fun setSoundsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.SOUNDS_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.VIBRATION_ENABLED] = enabled }
    }

    fun testNotification() {
        // no-op in common
    }
}
