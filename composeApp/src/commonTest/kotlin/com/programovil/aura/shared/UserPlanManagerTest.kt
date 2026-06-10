package com.programovil.aura.shared

import app.cash.turbine.test
import com.programovil.aura.experiments.domain.model.UserPlan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import java.util.concurrent.locks.LockSupport
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class UserPlanManagerTest {

    private suspend fun awaitFlag(
        flagManager: FeatureFlagManager,
        expected: Boolean
    ) {
        for (i in 1..30) {
            delay(50)
            LockSupport.parkNanos(1_000_000)
            if (flagManager.flags.value[FeatureFlag.IS_PREMIUM] == expected) return
        }
    }

    @Test
    fun `userPlan is Premium when IS_PREMIUM flag is true`() =
        runTest(UnconfinedTestDispatcher()) {
            val flagManager = FeatureFlagManager(
                FakeRemoteConfigService(
                    stringValues = mapOf(FeatureFlag.IS_PREMIUM.key to "true")
                )
            )
            flagManager.initialize()
            awaitFlag(flagManager, expected = true)
            val manager = UserPlanManager(flagManager)

            manager.userPlan.test {
                assertEquals(UserPlan.Premium, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `userPlan is Free when IS_PREMIUM flag is false`() =
        runTest(UnconfinedTestDispatcher()) {
            val flagManager = FeatureFlagManager(
                FakeRemoteConfigService(
                    stringValues = mapOf(FeatureFlag.IS_PREMIUM.key to "false")
                )
            )
            flagManager.initialize()
            awaitFlag(flagManager, expected = false)
            val manager = UserPlanManager(flagManager)

            manager.userPlan.test {
                assertEquals(UserPlan.Free, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `userPlan defaults to Free when IS_PREMIUM is missing`() =
        runTest(UnconfinedTestDispatcher()) {
            val flagManager = FeatureFlagManager(FakeRemoteConfigService())
            flagManager.initialize()
            val manager = UserPlanManager(flagManager)

            manager.userPlan.test {
                assertEquals(UserPlan.Free, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
