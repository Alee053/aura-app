package com.programovil.aura.shared

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

class FeatureFlagManager(
    private val remoteConfigService: RemoteConfigService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollingJob: Job? = null

    private val _flags = MutableStateFlow(
        FeatureFlag.entries.associateWith { it.defaultValue }
    )
    val flags: StateFlow<Map<FeatureFlag, Boolean>> = _flags.asStateFlow()

    suspend fun initialize() {
        remoteConfigService.fetchAndActivate()
        refreshFlags()

        remoteConfigService.registerOnConfigUpdateListener {
            scope.launch {
                remoteConfigService.fetchAndActivate()
                refreshFlags()
            }
        }

        startPolling()
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
                remoteConfigService.fetchAndActivate()
                refreshFlags()
            }
        }
    }

    private suspend fun refreshFlags() {
        _flags.value = FeatureFlag.entries.associateWith { flag ->
            remoteConfigService.getBoolean(flag)
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 30_000L // 30 seconds
    }
}
