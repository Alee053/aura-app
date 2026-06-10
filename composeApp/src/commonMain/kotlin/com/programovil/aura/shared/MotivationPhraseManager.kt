package com.programovil.aura.shared

import io.mockative.Mockable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Typed wrapper that exposes the current motivation phrase sourced from
 * the [StringRemoteConfigFlag.MOTIVATION_PHRASE] remote-config key.
 *
 * Hot-reloads on Firebase real-time updates and on the 5-second polling
 * fallback started by [initialize].
 */
@Mockable
class MotivationPhraseManager(
    private val remoteConfigService: RemoteConfigService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val manager = RemoteConfigValueManager(
        remoteConfigService = remoteConfigService,
        key = StringRemoteConfigFlag.MOTIVATION_PHRASE.key,
        defaultValue = StringRemoteConfigFlag.MOTIVATION_PHRASE.defaultValue,
        parser = { it }
    )

    val phrase: Flow<String> = manager.value

    suspend fun initialize() {
        manager.refresh()
        remoteConfigService.registerOnConfigUpdateListener {
            scope.launch { manager.refresh() }
        }
        scope.launch {
            while (isActive) {
                delay(5000)
                manager.refresh()
            }
        }
    }

    fun stop() {
        scope.cancel()
    }
}
