package com.programovil.aura.experiments.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.repository.UserPlanRepository
import com.programovil.aura.shared.RemoteConfigService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

private val USER_PLAN_KEY = stringPreferencesKey("user_plan")

class DataStoreUserPlanRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val remoteConfigService: RemoteConfigService
) : UserPlanRepository {

    override fun observeUserPlan(): Flow<UserPlan> =
        dataStore.data
            .map { prefs -> UserPlan.fromRemoteConfigString(prefs[USER_PLAN_KEY]) }
            .distinctUntilChanged()

    override suspend fun getUserPlan(): UserPlan = observeUserPlan().first()

    override suspend fun setUserPlan(plan: UserPlan) {
        dataStore.edit { prefs -> prefs[USER_PLAN_KEY] = plan.toRemoteString() }
    }

    override suspend fun refreshFromRemote(): Result<Unit> = runCatching {
        val raw = remoteConfigService.getUserPlan()
        val plan = UserPlan.fromRemoteConfigString(raw)
        dataStore.edit { prefs -> prefs[USER_PLAN_KEY] = plan.toRemoteString() }
    }

    private fun UserPlan.toRemoteString(): String = when (this) {
        UserPlan.Premium -> "Premium"
        UserPlan.Free -> "Free"
    }
}
