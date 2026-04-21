package com.programovil.aura.notification.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_preferences")

class NotificationPreferences(private val context: Context) {

    private object Keys {
        val DAILY_SUMMARY_ENABLED = booleanPreferencesKey("daily_summary_enabled")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
    }

    val dailySummaryEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[Keys.DAILY_SUMMARY_ENABLED] ?: true }

    val notificationHour: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[Keys.NOTIFICATION_HOUR] ?: 8 }

    val notificationMinute: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[Keys.NOTIFICATION_MINUTE] ?: 0 }

    suspend fun setDailySummaryEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.DAILY_SUMMARY_ENABLED] = enabled }
    }

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_HOUR] = hour
            prefs[Keys.NOTIFICATION_MINUTE] = minute
        }
    }
}