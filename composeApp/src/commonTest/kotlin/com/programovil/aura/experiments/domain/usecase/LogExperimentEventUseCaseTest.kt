package com.programovil.aura.experiments.domain.usecase

import com.programovil.aura.experiments.domain.model.ExperimentEvent
import com.programovil.aura.experiments.domain.model.UserPlan
import com.programovil.aura.experiments.domain.repository.ExperimentRepository
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class LogExperimentEventUseCaseTest {

    private val repository = mock(of<ExperimentRepository>())

    @Test
    fun `invoke delegates to repository logEvent`() = runTest {
        val event = ExperimentEvent.HomeOpened(UserPlan.Free)
        coEvery { repository.logEvent(event) } returns Result.success(Unit)
        val useCase = LogExperimentEventUseCase(repository)

        val result = useCase(event)
        coVerify { repository.logEvent(event) }.wasInvoked(exactly = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke surfaces failure from repository`() = runTest {
        val event = ExperimentEvent.SessionActive(UserPlan.Premium)
        coEvery { repository.logEvent(event) } returns Result.failure(RuntimeException("boom"))
        val useCase = LogExperimentEventUseCase(repository)

        val result = useCase(event)
        assertTrue(result.isFailure)
    }
}
