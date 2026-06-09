package com.programovil.aura.shared

import com.programovil.aura.experiments.domain.model.UserPlan
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserPlanManagerTest {

    @Test
    fun `initialize reads user_plan string and parses to UserPlan_Premium`() = runTest {
        val fake = FakeRemoteConfigService(
            stringValues = mapOf(UserPlanFlag.USER_PLAN.key to "Premium")
        )
        val manager = UserPlanManager(fake)

        manager.initialize()

        assertEquals(UserPlan.Premium, manager.userPlan.value)
    }

    @Test
    fun `initialize falls back to Free when remote returns unknown value`() = runTest {
        val fake = FakeRemoteConfigService(
            stringValues = mapOf(UserPlanFlag.USER_PLAN.key to "Garbage")
        )
        val manager = UserPlanManager(fake)

        manager.initialize()

        assertEquals(UserPlan.Free, manager.userPlan.value)
    }
}
