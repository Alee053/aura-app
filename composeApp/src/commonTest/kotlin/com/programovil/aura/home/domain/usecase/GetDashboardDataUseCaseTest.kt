package com.programovil.aura.home.domain.usecase

import app.cash.turbine.test
import com.programovil.aura.habit.domain.model.DayCompletion
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitWithStatus
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.usecase.GetHabitsWithStatusUseCase
import com.programovil.aura.home.domain.model.DashboardData
import com.programovil.aura.todo.domain.model.Todo
import com.programovil.aura.todo.domain.usecase.GetTodosUseCase
import io.mockative.every
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetDashboardDataUseCaseTest {

    private val getTodosUseCase = mock(of<GetTodosUseCase>())
    private val getHabitsUseCase = mock(of<GetHabitsWithStatusUseCase>())
    private val useCase = GetDashboardDataUseCase(getTodosUseCase, getHabitsUseCase)

    @Test
    fun `emits dashboard data computed from both sources`() = runTest {
        val todos = listOf(
            Todo("t-1", "A", null, isCompleted = false, dueDate = null),
            Todo("t-2", "B", null, isCompleted = true, dueDate = null),
            Todo("t-3", "C", null, isCompleted = false, dueDate = null)
        )
        val habits = listOf(
            HabitWithStatus(
                habit = sampleHabit("h-1", "Run"),
                currentPeriodProgress = 1 to 1,
                streak = 3,
                last7Days = listOf(DayCompletion("2026-06-10", isCompleted = true))
            ),
            HabitWithStatus(
                habit = sampleHabit("h-2", "Read"),
                currentPeriodProgress = 0 to 1,
                streak = 7,
                last7Days = listOf(DayCompletion("2026-06-10", isCompleted = false))
            ),
            HabitWithStatus(
                habit = sampleHabit("h-3", "Walk"),
                currentPeriodProgress = 0 to 1,
                streak = 1,
                last7Days = emptyList()
            )
        )
        every { getTodosUseCase() } returns flowOf(Result.success(todos))
        every { getHabitsUseCase() } returns flowOf(Result.success(habits))

        useCase().test {
            val result = awaitItem()
            assertEquals(
                DashboardData(
                    incompleteTodos = 2,
                    completedHabitsToday = 1,
                    totalHabitsToday = 3,
                    currentStreak = 7
                ),
                result.getOrNull()
            )
            awaitComplete()
        }
    }

    @Test
    fun `emits zero-valued dashboard when sources succeed with empty lists`() = runTest {
        every { getTodosUseCase() } returns flowOf(Result.success(emptyList()))
        every { getHabitsUseCase() } returns flowOf(Result.success(emptyList()))

        useCase().test {
            val result = awaitItem().getOrNull()
            assertEquals(
                DashboardData(
                    incompleteTodos = 0,
                    completedHabitsToday = 0,
                    totalHabitsToday = 0,
                    currentStreak = 0
                ),
                result
            )
            awaitComplete()
        }
    }

    @Test
    fun `failed sources propagate the todo error with deterministic precedence`() = runTest {
        val failure = RuntimeException("todo")
        every { getTodosUseCase() } returns flowOf(Result.failure(failure))
        every { getHabitsUseCase() } returns flowOf(Result.failure(RuntimeException("habit")))
        useCase().test {
            assertEquals(failure, awaitItem().exceptionOrNull())
            awaitComplete()
        }
    }

    @Test
    fun `one failed source is not misrepresented as a confirmed zero`() = runTest {
        every { getTodosUseCase() } returns flowOf(Result.success(emptyList()))
        every { getHabitsUseCase() } returns flowOf(Result.failure(RuntimeException("habit")))
        useCase().test {
            assertTrue(awaitItem().isFailure)
            awaitComplete()
        }
    }

    private fun sampleHabit(id: String, name: String) = Habit(
        id = id,
        name = name,
        recurrenceType = RecurrenceType.DAILY,
        targetCount = 1,
        color = "#000000",
        createdAt = 0L
    )
}
