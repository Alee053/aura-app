package com.programovil.aura.pomodoro.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.programovil.aura.pomodoro.domain.PomodoroMode
import com.programovil.aura.pomodoro.domain.PomodoroStateRepository
import com.programovil.aura.pomodoro.domain.PomodoroTimerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PomodoroPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : PomodoroStateRepository {

    private object Keys {
        val TIME_LEFT_SECONDS = intPreferencesKey("pomodoro_time_left_seconds")
        val INITIAL_TIME_SECONDS = intPreferencesKey("pomodoro_initial_time_seconds")
        val IS_RUNNING = booleanPreferencesKey("pomodoro_is_running")
        val MODE = stringPreferencesKey("pomodoro_mode")
        val SESSIONS_COMPLETED = intPreferencesKey("pomodoro_sessions_completed")
        val SELECTED_OPTION = intPreferencesKey("pomodoro_selected_option")
        val ENDS_AT_EPOCH_MILLIS = longPreferencesKey("pomodoro_ends_at_epoch_millis")
        val SHOW_COMPLETION_MESSAGE = booleanPreferencesKey("pomodoro_show_completion_message")
        val COMPLETED_MODE = stringPreferencesKey("pomodoro_completed_mode")
    }

    override val state: Flow<PomodoroTimerState> = dataStore.data.map { prefs ->
        PomodoroTimerState(
            timeLeftSeconds = prefs[Keys.TIME_LEFT_SECONDS] ?: PomodoroTimerState().timeLeftSeconds,
            initialTimeSeconds = prefs[Keys.INITIAL_TIME_SECONDS] ?: PomodoroTimerState().initialTimeSeconds,
            isRunning = prefs[Keys.IS_RUNNING] ?: false,
            mode = prefs[Keys.MODE]?.let(PomodoroMode::valueOf) ?: PomodoroMode.POMODORO,
            sessionsCompleted = prefs[Keys.SESSIONS_COMPLETED] ?: 0,
            selectedOption = prefs[Keys.SELECTED_OPTION] ?: PomodoroTimerState().selectedOption,
            endsAtEpochMillis = prefs[Keys.ENDS_AT_EPOCH_MILLIS],
            showCompletionMessage = prefs[Keys.SHOW_COMPLETION_MESSAGE] ?: false,
            completedMode = prefs[Keys.COMPLETED_MODE]?.let(PomodoroMode::valueOf)
        )
    }

    override suspend fun saveState(state: PomodoroTimerState) {
        dataStore.edit { prefs ->
            prefs[Keys.TIME_LEFT_SECONDS] = state.timeLeftSeconds
            prefs[Keys.INITIAL_TIME_SECONDS] = state.initialTimeSeconds
            prefs[Keys.IS_RUNNING] = state.isRunning
            prefs[Keys.MODE] = state.mode.name
            prefs[Keys.SESSIONS_COMPLETED] = state.sessionsCompleted
            prefs[Keys.SELECTED_OPTION] = state.selectedOption
            prefs[Keys.SHOW_COMPLETION_MESSAGE] = state.showCompletionMessage

            if (state.endsAtEpochMillis == null) {
                prefs.remove(Keys.ENDS_AT_EPOCH_MILLIS)
            } else {
                prefs[Keys.ENDS_AT_EPOCH_MILLIS] = state.endsAtEpochMillis
            }

            if (state.completedMode == null) {
                prefs.remove(Keys.COMPLETED_MODE)
            } else {
                prefs[Keys.COMPLETED_MODE] = state.completedMode.name
            }
        }
    }
}
