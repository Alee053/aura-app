package com.programovil.aura.shared

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Typed wrapper that exposes a snapshot [Map] of every [FeatureFlag] sourced
 * from Remote Config. Internally composes one [RemoteConfigValueManager] per
 * flag entry; no polling loop is started.
 */
class FeatureFlagManager(
    remoteConfigService: RemoteConfigService
) {
    private val managers: Map<FeatureFlag, RemoteConfigValueManager<Boolean>> =
        FeatureFlag.entries.associateWith { flag ->
            RemoteConfigValueManager(
                remoteConfigService = remoteConfigService,
                key = flag.key,
                defaultValue = flag.defaultValue,
                parser = { raw -> raw.toBooleanStrictOrNull() ?: flag.defaultValue }
            )
        }

    private val _flags = MutableStateFlow(
        FeatureFlag.entries.associateWith { it.defaultValue }
    )
    val flags: StateFlow<Map<FeatureFlag, Boolean>> = _flags.asStateFlow()

    suspend fun initialize() = coroutineScope {
        managers.values.forEach { it.initialize() }
        managers.forEach { (flag, mgr) ->
            launch {
                mgr.value.collect { newValue ->
                    _flags.update { it + (flag to newValue) }
                }
            }
        }
    }

    fun stop() {
        managers.values.forEach { it.stop() }
    }
}
