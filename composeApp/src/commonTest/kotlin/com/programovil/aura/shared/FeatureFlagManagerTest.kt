package com.programovil.aura.shared

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.util.concurrent.locks.LockSupport
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [FeatureFlagManager].
 *
 * The composed [RemoteConfigValueManager] instances launch their internal
 * `refresh()` work on `Dispatchers.Default` (a real thread pool that
 * `runTest` does not control). Because [FeatureFlagManager.initialize]
 * blocks indefinitely on a `collect` scope, the test launches `initialize`
 * in the background and polls the resulting flags until the remote values
 * are reflected. A `LockSupport.parkNanos` between polls forces a JVM-level
 * thread yield so the `Dispatchers.Default` coroutines get a deterministic
 * chance to run.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FeatureFlagManagerTest {

    private suspend fun awaitRemoteFlags(
        manager: FeatureFlagManager,
        expectedHabits: Boolean,
        expectedTodos: Boolean
    ): Map<FeatureFlag, Boolean> {
        for (i in 1..30) {
            delay(50)
            LockSupport.parkNanos(1_000_000)
            val v = manager.flags.value
            if (v[FeatureFlag.HABITS_ENABLED] == expectedHabits &&
                v[FeatureFlag.TODOS_ENABLED] == expectedTodos
            ) {
                return v
            }
        }
        return manager.flags.value
    }

    @Test
    fun `initialize fetches flags and emits remote values`() =
        runTest(UnconfinedTestDispatcher()) {
            val manager = FeatureFlagManager(FakeRemoteConfigService(
                stringValues = mapOf(
                    FeatureFlag.HABITS_ENABLED.key to "true",
                    FeatureFlag.TODOS_ENABLED.key to "true"
                )
            ))

            assertEquals(
                FeatureFlag.entries.associateWith { it.defaultValue },
                manager.flags.value
            )

            backgroundScope.launch { manager.initialize() }

            val flags = awaitRemoteFlags(manager, expectedHabits = true, expectedTodos = true)
            assertEquals(true, flags[FeatureFlag.HABITS_ENABLED])
            assertEquals(true, flags[FeatureFlag.TODOS_ENABLED])
            assertEquals(FeatureFlag.JOURNAL_ENABLED.defaultValue, flags[FeatureFlag.JOURNAL_ENABLED])
        }

    @Test
    fun `initialize reads remote-disabled flags as false`() =
        runTest(UnconfinedTestDispatcher()) {
            val manager = FeatureFlagManager(FakeRemoteConfigService(
                stringValues = mapOf(
                    FeatureFlag.HABITS_ENABLED.key to "false",
                    FeatureFlag.TODOS_ENABLED.key to "false"
                )
            ))

            assertEquals(
                FeatureFlag.entries.associateWith { it.defaultValue },
                manager.flags.value
            )

            backgroundScope.launch { manager.initialize() }

            val flags = awaitRemoteFlags(manager, expectedHabits = false, expectedTodos = false)
            assertEquals(false, flags[FeatureFlag.HABITS_ENABLED])
            assertEquals(false, flags[FeatureFlag.TODOS_ENABLED])
            assertEquals(FeatureFlag.JOURNAL_ENABLED.defaultValue, flags[FeatureFlag.JOURNAL_ENABLED])
        }
}
