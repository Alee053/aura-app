package com.programovil.aura.shared

import android.util.Log
import com.programovil.aura.experiments.domain.model.UserPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Mirrors the [FeatureFlagManager] pattern for the [UserPlan] value, which is a String
 * (not a Boolean) parameter sourced from Remote Config. Keeps the local plan in sync with
 * the cloud value via:
 *   1. fetch-and-activate on [initialize]
 *   2. real-time listener (registerOnConfigUpdateListener)
 *   3. 30-second polling as a safety net
 */
class UserPlanManager(
    private val remoteConfigService: RemoteConfigService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollingJob: Job? = null

    private val _userPlan: MutableStateFlow<UserPlan> = MutableStateFlow(UserPlan.Free)
    val userPlan: StateFlow<UserPlan> = _userPlan.asStateFlow()

    suspend fun initialize() {
        Log.d(TAG, "UserPlanManager.initialize() — fetching from Remote Config")
        remoteConfigService.fetchAndActivate()
        refresh()

        remoteConfigService.registerOnConfigUpdateListener {
            Log.d(TAG, "UserPlanManager: real-time update from Remote Config — re-fetching")
            scope.launch {
                remoteConfigService.fetchAndActivate()
                refresh()
            }
        }

        startPolling()
        Log.d(TAG, "UserPlanManager initialized, current value: ${_userPlan.value}")
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        scope.cancel()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val fetchResult = remoteConfigService.fetchAndActivate()
                Log.d(TAG, "UserPlanManager: polling tick, fetchResult=$fetchResult")
                refresh()
            }
        }
    }

    private suspend fun refresh() {
        val raw = remoteConfigService.getUserPlan()
        val plan = UserPlan.fromRemoteConfigString(raw)
        if (plan != _userPlan.value) {
            Log.d(TAG, "UserPlanManager: plan changed from ${_userPlan.value} to $plan (raw=\"$raw\")")
        }
        _userPlan.value = plan
    }

    companion object {
        private const val TAG = "UserPlanManager"
        private const val POLL_INTERVAL_MS = 30_000L // 30 seconds
    }
}
