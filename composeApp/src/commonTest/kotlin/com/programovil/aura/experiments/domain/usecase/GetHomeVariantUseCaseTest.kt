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

class GetHomeVariantUseCaseTest {

    private val repository = mock(of<UserPlanRepository>())
    private val getUserPlanUseCase = GetUserPlanUseCase(repository)
    private val useCase = GetHomeVariantUseCase(getUserPlanUseCase)

    @Test
    fun `Free plan returns variant with no daily motivation`() = runTest {
        coEvery { repository.getUserPlan() } returns UserPlan.Free

        val variant = useCase()
        assertFalse(variant.showsDailyMotivation)
        assertEquals(Tone.Gentle, variant.tone)
    }

    @Test
    fun `Premium plan returns variant with daily motivation and direct tone`() = runTest {
        coEvery { repository.getUserPlan() } returns UserPlan.Premium

        val variant = useCase()
        assertTrue(variant.showsDailyMotivation)
        assertEquals(Tone.Direct, variant.tone)
    }
}
