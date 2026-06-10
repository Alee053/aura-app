package com.programovil.aura.experiments.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.repository.UserPlanRepository
import com.programovil.aura.shared.UserPlanManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

private val USER_PLAN_KEY = stringPreferencesKey("user_plan")
private const val TAG = "UserPlanRepository"

/**
 * Single source of truth for the user plan, with this precedence:
 *   1. If [DataStore] has an explicit value (set via the debug toggle), that wins.
 *   2. Otherwise, the value from [UserPlanManager] (which is fed by Remote Config).
 *
 * The DataStore is the "local override" layer — explicit user actions (debug toggle,
 * future payment integration) take precedence over Remote Config defaults.
 */
class DataStoreUserPlanRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val userPlanManager: UserPlanManager
) : UserPlanRepository {

    override fun observeUserPlan(): Flow<UserPlan> {
        val fromDataStore: Flow<UserPlan?> = dataStore.data
            .map { prefs -> prefs[USER_PLAN_KEY]?.let { UserPlan.fromRemoteConfigString(it) } }
            .distinctUntilChanged()
        return combine(fromDataStore, userPlanManager.userPlan) { override, fromRemote ->
            override ?: fromRemote
        }
    }

    override suspend fun getUserPlan(): UserPlan = observeUserPlan().first()

    override suspend fun setUserPlan(plan: UserPlan) {
        Log.d(TAG, "setUserPlan($plan) called — writing to DataStore as local override")
        dataStore.edit { prefs -> prefs[USER_PLAN_KEY] = plan.toRemoteString() }
    }

    override suspend fun refreshFromRemote(): Result<Unit> = runCatching {
        userPlanManager.initialize()
    }

    private fun UserPlan.toRemoteString(): String = when (this) {
        UserPlan.Premium -> "Premium"
        UserPlan.Free -> "Free"
    }
}
