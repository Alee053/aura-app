package com.programovil.aura.shared

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FeatureFlagManagerTest {

    @Test
    fun `initialize fetches flags and emits defaults when all true`() = runTest {
        val manager = FeatureFlagManager(FakeRemoteConfigService(
            fetchResult = Result.success(Unit),
            booleanValues = mapOf(
                FeatureFlag.HABITS_ENABLED to true,
                FeatureFlag.TODOS_ENABLED to true
            )
        ))
        manager.initialize()

        val flags = manager.flags.value
        assertEquals(true, flags[FeatureFlag.HABITS_ENABLED])
        assertEquals(true, flags[FeatureFlag.TODOS_ENABLED])
    }

    @Test
    fun `initialize handles fetch failure and still registers listener`() = runTest {
        val manager = FeatureFlagManager(FakeRemoteConfigService(
            fetchResult = Result.failure(Exception("network error")),
            booleanValues = mapOf(
                FeatureFlag.HABITS_ENABLED to true,
                FeatureFlag.TODOS_ENABLED to true
            )
        ))
        manager.initialize()

        val flags = manager.flags.value
        assertEquals(true, flags[FeatureFlag.HABITS_ENABLED])
        assertEquals(true, flags[FeatureFlag.TODOS_ENABLED])
    }

    @Test
    fun `initialize reads boolean values from RemoteConfigService`() = runTest {
        val manager = FeatureFlagManager(FakeRemoteConfigService(
            fetchResult = Result.success(Unit),
            booleanValues = mapOf(
                FeatureFlag.HABITS_ENABLED to false,
                FeatureFlag.TODOS_ENABLED to false
            )
        ))
        manager.initialize()

        val flags = manager.flags.value
        assertEquals(false, flags[FeatureFlag.HABITS_ENABLED])
        assertEquals(false, flags[FeatureFlag.TODOS_ENABLED])
    }
}

private class FakeRemoteConfigService(
    private val fetchResult: Result<Unit>,
    private val booleanValues: Map<FeatureFlag, Boolean> = FeatureFlag.entries.associateWith { it.defaultValue }
) : RemoteConfigService {
    override suspend fun getBoolean(flag: FeatureFlag): Boolean = booleanValues[flag] ?: flag.defaultValue
    override suspend fun getString(flag: FeatureFlag, default: String): String = default
    override suspend fun getUserPlan(): String = UserPlanFlag.USER_PLAN.defaultValue
    override suspend fun fetchAndActivate(): Result<Unit> = fetchResult
    override fun registerOnConfigUpdateListener(onUpdate: () -> Unit) {}
}