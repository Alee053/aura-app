package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.repository.HabitRepository
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeleteHabitUseCaseTest {

    private val repository = mock(of<HabitRepository>())
    private val useCase = DeleteHabitUseCase(repository)

    @Test
    fun `invoke returns success when repository succeeds`() = runTest {
        coEvery { repository.deleteHabit("h-1") } returns Result.success(Unit)

        val result = useCase("h-1")

        assertTrue(result.isSuccess)
        coVerify { repository.deleteHabit("h-1") }.wasInvoked(exactly = 1)
    }

    @Test
    fun `invoke propagates failure from repository`() = runTest {
        val error = RuntimeException("not found")
        coEvery { repository.deleteHabit("h-2") } returns Result.failure(error)

        val result = useCase("h-2")

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
