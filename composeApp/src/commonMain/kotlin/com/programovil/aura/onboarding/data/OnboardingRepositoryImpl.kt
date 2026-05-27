package com.programovil.aura.onboarding.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.programovil.aura.onboarding.domain.OnboardingRepository
import com.programovil.aura.onboarding.domain.model.OnboardingConfig
import com.programovil.aura.onboarding.domain.model.OnboardingSlide
import com.programovil.aura.shared.RemoteConfigService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class OnboardingRepositoryImpl(
    private val remoteConfigService: RemoteConfigService,
    private val dataStore: DataStore<Preferences>
) : OnboardingRepository {

    private val onboardingSeenKey = booleanPreferencesKey("onboarding_seen")
    private val json = Json { ignoreUnknownKeys = true }

    override fun getSlides(): Flow<Result<List<OnboardingSlide>>> = flow {
        remoteConfigService.fetchAndActivate()
        val jsonString = remoteConfigService.getString("onboarding_config")
        if (jsonString.isNullOrEmpty()) {
            emit(Result.failure(Exception("Onboarding config not found in Remote Config")))
            return@flow
        }
        try {
            val slides = json.decodeFromString<List<OnboardingSlide>>(jsonString)
            emit(Result.success(slides))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun hasSeenOnboarding(): Boolean {
        return dataStore.data.first()[onboardingSeenKey] ?: false
    }

    override suspend fun setOnboarded(seen: Boolean) {
        dataStore.edit { preferences ->
            preferences[onboardingSeenKey] = seen
        }
    }
}
