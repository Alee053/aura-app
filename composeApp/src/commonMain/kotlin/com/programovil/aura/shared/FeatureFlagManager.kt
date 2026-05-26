package com.programovil.aura.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeatureFlagManager(
    private val remoteConfigService: RemoteConfigService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _flags = MutableStateFlow(
        FeatureFlag.entries.associateWith { it.defaultValue }
    )
    val flags: StateFlow<Map<FeatureFlag, Boolean>> = _flags.asStateFlow()

    suspend fun initialize() {
        remoteConfigService.fetchAndActivate()
        refreshFlags()

        remoteConfigService.registerOnConfigUpdateListener {
            scope.launch { refreshFlags() }
        }
    }

    private suspend fun refreshFlags() {
        _flags.value = FeatureFlag.entries.associateWith { flag ->
            remoteConfigService.getBoolean(flag)
        }
    }
}
