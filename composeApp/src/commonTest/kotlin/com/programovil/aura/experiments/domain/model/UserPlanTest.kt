package com.programovil.aura.experiments.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class UserPlanTest {

    @Test
    fun `fromRemoteConfigBoolean true maps to Premium`() {
        assertEquals(UserPlan.Premium, UserPlan.fromRemoteConfigBoolean(true))
    }

    @Test
    fun `fromRemoteConfigBoolean false maps to Free`() {
        assertEquals(UserPlan.Free, UserPlan.fromRemoteConfigBoolean(false))
    }

    @Test
    fun `fromRemoteConfigBoolean null maps to Free (safe default)`() {
        assertEquals(UserPlan.Free, UserPlan.fromRemoteConfigBoolean(null))
    }
}
