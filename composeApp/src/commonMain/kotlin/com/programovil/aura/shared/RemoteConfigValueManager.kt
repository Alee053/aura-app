package com.programovil.aura.shared

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Generic holder for a single value sourced from [RemoteConfigService].
 *
 * Owns the fetch + real-time listener + lifecycle pattern. Typed wrappers
 * (e.g. [FeatureFlagManager], [UserPlanManager]) compose one or more of these.
 *
 * Lifecycle:
 * - Call [initialize] once at startup, then read [value].
 * - Call [stop] to cancel the internal scope. After [stop] the manager is dead.
 * - Calling [refresh] after [stop] is a silent no-op.
 * - Calling [refresh] before [initialize] is permitted but the fetched value
 *   is not reflected in [value] until the listener is registered; in practice
 *   [refresh] is only useful inside [initialize] or as an on-resume re-fetch.
 *
 * No polling loop is started: Firebase's real-time
 * [RemoteConfigService.registerOnConfigUpdateListener] is the only source of
 * updates after the initial fetch.
 */
class RemoteConfigValueManager<T>(
    private val remoteConfigService: RemoteConfigService,
    private val key: String,
    private val defaultValue: T,
    private val parser: (String) -> T
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _value = MutableStateFlow(defaultValue)
    val value: StateFlow<T> = _value.asStateFlow()

    suspend fun initialize() {
        refresh()
        remoteConfigService.registerOnConfigUpdateListener {
            scope.launch { refresh() }
        }
    }

    fun refresh() {
        scope.launch {
            remoteConfigService.fetchAndActivate()
            val raw = remoteConfigService.getString(key, defaultValue.toString())
            _value.value = parser(raw)
        }
    }

    fun stop() {
        scope.cancel()
    }
}
