package com.programovil.aura.experiments.domain.usecase

import app.cash.turbine.test
import com.programovil.aura.experiments.domain.model.Tone
import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.repository.UserPlanRepository
import io.mockative.every
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetHomeVariantUseCaseTest {

    private val repository = mock(of<UserPlanRepository>())
    private val getMotivationPhraseUseCase = mock(of<GetMotivationPhraseUseCase>())
    private val getUserPlanUseCase = GetUserPlanUseCase(repository)
    private val useCase = GetHomeVariantUseCase(getUserPlanUseCase, getMotivationPhraseUseCase)

    @Test
    fun `Free plan returns variant with no daily motivation`() = runTest {
        every { repository.observeUserPlan() } returns MutableStateFlow(UserPlan.Free)
        every { getMotivationPhraseUseCase() } returns MutableStateFlow("Any phrase")

        useCase().test {
            val variant = awaitItem()
            assertFalse(variant.showsDailyMotivation)
            assertEquals(Tone.Gentle, variant.tone)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Premium plan returns variant with daily motivation and direct tone`() = runTest {
        every { repository.observeUserPlan() } returns MutableStateFlow(UserPlan.Premium)
        every { getMotivationPhraseUseCase() } returns MutableStateFlow("Any phrase")

        useCase().test {
            val variant = awaitItem()
            assertTrue(variant.showsDailyMotivation)
            assertEquals(Tone.Direct, variant.tone)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `motivation phrase flows through to HomeVariant`() = runTest {
        every { repository.observeUserPlan() } returns MutableStateFlow(UserPlan.Premium)
        every { getMotivationPhraseUseCase() } returns MutableStateFlow("Hello from RC")

        useCase().test {
            val variant = awaitItem()
            assertEquals("Hello from RC", variant.motivationPhrase)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
