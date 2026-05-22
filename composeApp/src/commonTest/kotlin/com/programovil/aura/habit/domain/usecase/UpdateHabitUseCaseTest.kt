package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.repository.HabitRepository
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class UpdateHabitUseCaseTest {

    private val repository = mock(of<HabitRepository>())
    private val useCase = UpdateHabitUseCase(repository)

    @Test
    fun `invoke updates habit successfully`() = runTest {
        val habit = Habit(
            id = "h1",
            name = "Exercise",
            recurrenceType = RecurrenceType.DAILY,
            daysOfWeek = emptyList(),
            color = "#FF6B6B"
        )
        coEvery { repository.updateHabit(habit) } returns Result.success(Unit)

        val result = useCase(habit)

        assertTrue(result.isSuccess)
        coVerify { repository.updateHabit(habit) }.wasInvoked(exactly = 1)
    }

    @Test
    fun `invoke returns failure when repository fails`() = runTest {
        val habit = Habit(
            id = "h1",
            name = "Exercise",
            recurrenceType = RecurrenceType.DAILY,
            daysOfWeek = emptyList(),
            color = "#FF6B6B"
        )
        coEvery { repository.updateHabit(habit) } returns Result.failure(Exception("DB error"))

        val result = useCase(habit)

        assertTrue(result.isFailure)
    }
}