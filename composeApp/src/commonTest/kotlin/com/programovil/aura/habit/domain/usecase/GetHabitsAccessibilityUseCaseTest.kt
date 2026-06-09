package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.usecase.GetUserPlanUseCase
import com.programovil.aura.shared.FeatureFlag
import com.programovil.aura.shared.FeatureFlagManager
import io.mockative.every
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetHabitsAccessibilityUseCaseTest {

    private val featureFlagManager = mock(of<FeatureFlagManager>())
    private val getUserPlanUseCase = mock(of<GetUserPlanUseCase>())
    private val useCase = GetHabitsAccessibilityUseCase(featureFlagManager, getUserPlanUseCase)

    @Test
    fun `flag true and Premium plan returns true`() = runTest {
        every { featureFlagManager.flags } returns MutableStateFlow(
            mapOf(FeatureFlag.HABITS_ENABLED to true)
        )
        every { getUserPlanUseCase() } returns flowOf(UserPlan.Premium)

        val result = useCase().let { flow ->
            var collected = false
            var value = false
            flow.collect {
                collected = true
                value = it
            }
            if (!collected) error("use case emitted no value")
            value
        }

        assertEquals(true, result)
    }

    @Test
    fun `flag true and Free plan returns false`() = runTest {
        every { featureFlagManager.flags } returns MutableStateFlow(
            mapOf(FeatureFlag.HABITS_ENABLED to true)
        )
        every { getUserPlanUseCase() } returns flowOf(UserPlan.Free)

        val collected = mutableListOf<Boolean>()
        useCase().collect { collected += it }

        assertEquals(listOf(false), collected)
    }

    @Test
    fun `flag false returns false regardless of plan`() = runTest {
        every { featureFlagManager.flags } returns MutableStateFlow(
            mapOf(FeatureFlag.HABITS_ENABLED to false)
        )
        every { getUserPlanUseCase() } returns flowOf(UserPlan.Premium)

        val collected = mutableListOf<Boolean>()
        useCase().collect { collected += it }

        assertEquals(listOf(false), collected)
    }
}
