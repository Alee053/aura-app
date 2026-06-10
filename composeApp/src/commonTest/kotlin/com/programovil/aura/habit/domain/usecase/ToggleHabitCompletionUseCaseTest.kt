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

class ToggleHabitCompletionUseCaseTest {

    private val repository = mock(of<HabitRepository>())
    private val useCase = ToggleHabitCompletionUseCase(repository)

    @Test
    fun `invoke forwards habitId and date to repository`() = runTest {
        coEvery { repository.toggleCompletion("h-1", "2026-06-10") } returns Result.success(Unit)

        val result = useCase("h-1", "2026-06-10")

        assertTrue(result.isSuccess)
        coVerify { repository.toggleCompletion("h-1", "2026-06-10") }.wasInvoked(exactly = 1)
    }

    @Test
    fun `invoke returns failure when habitId is blank`() = runTest {
        val result = useCase("", "2026-06-10")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `invoke returns failure when date is blank`() = runTest {
        val result = useCase("h-1", "")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `invoke propagates repository failure`() = runTest {
        val error = RuntimeException("write conflict")
        coEvery { repository.toggleCompletion("h-1", "2026-06-10") } returns Result.failure(error)

        val result = useCase("h-1", "2026-06-10")

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
