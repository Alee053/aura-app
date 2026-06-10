package com.programovil.aura.shared

class StubRemoteConfigService : RemoteConfigService {
    override suspend fun getBoolean(key: String, default: Boolean): Boolean = default
    override suspend fun getString(key: String, default: String): String = default
    override suspend fun fetchAndActivate(): Result<Unit> = Result.success(Unit)
    override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) {
    }
}
