package com.programovil.aura.experiments.domain.usecase

import com.programovil.aura.experiments.domain.model.Tone
import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.repository.UserPlanRepository
import io.mockative.coEvery
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetNotificationVariantUseCaseTest {

    private val repository = mock(of<UserPlanRepository>())
    private val getUserPlanUseCase = GetUserPlanUseCase(repository)
    private val useCase = GetNotificationVariantUseCase(getUserPlanUseCase)

    @Test
    fun `Free plan returns 1 daily notification gentle tone no due-date channel`() = runTest {
        coEvery { repository.getUserPlan() } returns UserPlan.Free

        val variant = useCase()
        assertEquals(1, variant.timesPerDay)
        assertEquals(Tone.Gentle, variant.tone)
        assertFalse(variant.useDueDateChannel)
    }

    @Test
    fun `Premium plan returns 2 daily notifications direct tone with due-date channel`() = runTest {
        coEvery { repository.getUserPlan() } returns UserPlan.Premium

        val variant = useCase()
        assertEquals(2, variant.timesPerDay)
        assertEquals(Tone.Direct, variant.tone)
        assertTrue(variant.useDueDateChannel)
    }
}
