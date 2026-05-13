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

    }

    val dailySummaryEnabled: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[Keys.DAILY_SUMMARY_ENABLED] ?: false }

    val notificationHour: Flow<Int> = dataStore.data
        .map { prefs -> prefs[Keys.NOTIFICATION_HOUR] ?: 8 }

    val notificationMinute: Flow<Int> = dataStore.data
        .map { prefs -> prefs[Keys.NOTIFICATION_MINUTE] ?: 0 }

    suspend fun setDailySummaryEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DAILY_SUMMARY_ENABLED] = enabled }
    }

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_HOUR] = hour
            prefs[Keys.NOTIFICATION_MINUTE] = minute
        }
    }

    fun testNotification() {
        // no-op in common
    }
}
