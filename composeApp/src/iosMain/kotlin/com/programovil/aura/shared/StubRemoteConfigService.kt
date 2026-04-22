package com.programovil.aura.shared

class StubRemoteConfigService : RemoteConfigService {
    override suspend fun getBoolean(flag: FeatureFlag): Boolean = flag.defaultValue
    override suspend fun getString(flag: FeatureFlag, default: String): String = default
    override suspend fun fetchAndActivate(): Result<Unit> = Result.success(Unit)
}
