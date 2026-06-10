package com.programovil.aura.experiments.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class UserPlanTest {

    @Test
    fun `fromRemoteConfigString Premium maps to Premium`() {
        assertEquals(UserPlan.Premium, UserPlan.fromRemoteConfigString("Premium"))
    }

    @Test
    fun `fromRemoteConfigString Free maps to Free`() {
        assertEquals(UserPlan.Free, UserPlan.fromRemoteConfigString("Free"))
    }

    @Test
    fun `fromRemoteConfigString null maps to Free (safe default)`() {
        assertEquals(UserPlan.Free, UserPlan.fromRemoteConfigString(null))
    }

    @Test
    fun `fromRemoteConfigString unknown value maps to Free`() {
        assertEquals(UserPlan.Free, UserPlan.fromRemoteConfigString("Platinum"))
    }

    @Test
    fun `fromRemoteConfigString case-sensitive rejects lowercase premium`() {
        assertEquals(UserPlan.Free, UserPlan.fromRemoteConfigString("premium"))
    }
}
