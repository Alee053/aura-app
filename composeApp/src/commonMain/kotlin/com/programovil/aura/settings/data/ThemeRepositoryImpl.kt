package com.programovil.aura.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.programovil.aura.designsystem.theme.ThemeMode
import com.programovil.aura.settings.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemeRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : ThemeRepository {

    private val themeModeKey = stringPreferencesKey("theme_mode")

    override fun getThemeMode(): Flow<ThemeMode> {
        return dataStore.data.map { prefs ->
            val name = prefs[themeModeKey] ?: ThemeMode.PURPLE.name
            ThemeMode.valueOf(name)
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }
}
