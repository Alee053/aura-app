package com.programovil.aura.shared

interface RemoteConfigService {
    suspend fun getBoolean(flag: FeatureFlag): Boolean
    suspend fun getString(flag: FeatureFlag, default: String): String
    suspend fun fetchAndActivate(): Result<Unit>
}