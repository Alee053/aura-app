package com.programovil.aura.onboarding.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface OnboardingPreferences {
    val isOnboardingCompleted: Flow<Boolean>
    suspend fun setOnboardingCompleted()
}

class OnboardingPreferencesImpl(private val dataStore: DataStore<Preferences>) : OnboardingPreferences {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    override val isOnboardingCompleted: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[Keys.ONBOARDING_COMPLETED] ?: false }

    override suspend fun setOnboardingCompleted() {
        dataStore.edit { prefs -> prefs[Keys.ONBOARDING_COMPLETED] = true }
    }
}