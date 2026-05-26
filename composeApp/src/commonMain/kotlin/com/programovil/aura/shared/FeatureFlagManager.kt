package com.programovil.aura.shared

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FeatureFlagManager(
    private val remoteConfigService: RemoteConfigService
) {
    private val _flags = MutableStateFlow(
        FeatureFlag.entries.associateWith { it.defaultValue }
    )
    val flags: StateFlow<Map<FeatureFlag, Boolean>> = _flags.asStateFlow()

    suspend fun initialize() {
        remoteConfigService.fetchAndActivate()
        refreshFlags()

        remoteConfigService.registerOnConfigUpdateListener {
            refreshFlags()
        }
    }

    private fun refreshFlags() {
        _flags.value = FeatureFlag.entries.associateWith { flag ->
            remoteConfigService.getBoolean(flag)
        }
    }
}
