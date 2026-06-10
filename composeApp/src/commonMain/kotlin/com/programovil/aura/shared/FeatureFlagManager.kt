package com.programovil.aura.shared

import io.mockative.Mockable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
@Mockable
class FeatureFlagManager(
    private val remoteConfigService: RemoteConfigService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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

    suspend fun initialize() {
        managers.values.forEach { it.refresh() }
        remoteConfigService.registerOnConfigUpdateListener {
            scope.launch {
                println("[FeatureFlagManager] Config update received, refreshing all flags...")
                managers.values.forEach { it.refresh() }
                println("[FeatureFlagManager] All flags refreshed")
            }
        }
        managers.forEach { (flag, mgr) ->
            scope.launch {
                mgr.value.collect { newValue ->
                    println("[FeatureFlagManager] Flag ${flag.key} updated to $newValue")
                    _flags.update { it + (flag to newValue) }
                }
            }
        }
    }

    fun stop() {
        scope.cancel()
        managers.values.forEach { it.stop() }
    }
}
