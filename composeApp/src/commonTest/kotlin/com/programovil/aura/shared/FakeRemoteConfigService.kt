package com.programovil.aura.shared

class FakeRemoteConfigService(
    private val fetchResult: Result<Unit> = Result.success(Unit),
    private val stringValues: Map<String, String> = emptyMap(),
    private val booleanValues: Map<String, Boolean> = emptyMap()
) : RemoteConfigService {
    override suspend fun getBoolean(key: String, default: Boolean): Boolean =
        booleanValues[key] ?: default
    override suspend fun getString(key: String, default: String): String =
        stringValues[key] ?: default
    override suspend fun fetchAndActivate(): Result<Unit> = fetchResult
    override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) {}
}
