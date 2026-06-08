package com.programovil.aura.shared

import io.mockative.Mockable

@Mockable
interface RemoteConfigService {
    suspend fun getBoolean(flag: FeatureFlag): Boolean
    suspend fun getString(flag: FeatureFlag, default: String): String
    suspend fun getUserPlan(): String
    suspend fun fetchAndActivate(): Result<Unit>

    /** Register a callback that is invoked when remote config values change. */
    fun registerOnConfigUpdateListener(onUpdate: () -> Unit)
}