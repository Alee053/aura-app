package com.programovil.aura.experiments.domain.usecase

import app.cash.turbine.test
import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.repository.UserPlanRepository
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.every
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetUserPlanUseCaseTest {

    private val repository = mock(of<UserPlanRepository>())

    @Test
    fun `invoke exposes repository observeUserPlan flow`() = runTest {
        every { repository.observeUserPlan() } returns flowOf(UserPlan.Free, UserPlan.Premium)
        val useCase = GetUserPlanUseCase(repository)

        useCase().test {
            assertEquals(UserPlan.Free, awaitItem())
            assertEquals(UserPlan.Premium, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `get returns current user plan from repository`() = runTest {
        coEvery { repository.getUserPlan() } returns UserPlan.Premium
        val useCase = GetUserPlanUseCase(repository)

        assertEquals(UserPlan.Premium, useCase.get())
    }

    @Test
    fun `set delegates to repository`() = runTest {
        coEvery { repository.setUserPlan(UserPlan.Premium) } returns Unit
        val useCase = GetUserPlanUseCase(repository)

        useCase.set(UserPlan.Premium)
        coVerify { repository.setUserPlan(UserPlan.Premium) }.wasInvoked(exactly = 1)
        assertTrue(true)
    }

    @Test
    fun `refresh returns Result from repository`() = runTest {
        coEvery { repository.refreshFromRemote() } returns Result.success(Unit)
        val useCase = GetUserPlanUseCase(repository)

        val result = useCase.refresh()
        assertTrue(result.isSuccess)
    }
}
