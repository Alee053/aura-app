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
        val updatedFlags = FeatureFlag.entries.associateWith { flag ->
            remoteConfigService.getBoolean(flag)
        }
        _flags.value = updatedFlags
    }
}