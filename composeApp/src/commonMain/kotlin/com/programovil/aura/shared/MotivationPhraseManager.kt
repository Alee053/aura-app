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
import kotlinx.serialization.json.Json

/**
 * Typed wrapper that exposes the current motivation phrase (locale-keyed map)
 * sourced from the [StringRemoteConfigFlag.MOTIVATION_PHRASE] remote-config key.
 *
 * Wire format is JSON: `{"en":"...","es":"...","fr":"..."}`. Hot-reloads on
 * Firebase real-time updates and on the 5-second polling fallback started by
 * [initialize].
 */
@Mockable
class MotivationPhraseManager(
    private val remoteConfigService: RemoteConfigService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val manager = RemoteConfigValueManager<Map<String, String>>(
        remoteConfigService = remoteConfigService,
        key = StringRemoteConfigFlag.MOTIVATION_PHRASE.key,
        defaultValue = emptyMap(),
        parser = { raw -> parseOrEmpty(raw) }
    )

    val phrase: Flow<Map<String, String>> = manager.value

    private fun parseOrEmpty(raw: String): Map<String, String> = try {
        json.decodeFromString(MotivationPhraseDto.serializer(), raw).phrases
    } catch (e: Exception) {
        println("[MotivationPhraseManager] Failed to parse JSON: $raw")
        emptyMap()
    }

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
