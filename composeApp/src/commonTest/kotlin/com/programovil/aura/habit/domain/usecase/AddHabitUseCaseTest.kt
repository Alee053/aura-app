package com.programovil.aura.habit.domain.usecase

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.model.RecurrenceType
import com.programovil.aura.habit.domain.repository.HabitRepository
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddHabitUseCaseTest {

    @Test
    fun `invoke returns failure when name is blank`() = runTest {
        val repository = CapturingHabitRepository()
        val useCase = AddHabitUseCase(repository)

        val result = useCase(name = "", RecurrenceType.DAILY, 1, "#000000")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(0, repository.added.size)
    }

    @Test
    fun `invoke returns failure when name is whitespace only`() = runTest {
        val repository = CapturingHabitRepository()
        val useCase = AddHabitUseCase(repository)

        val result = useCase(name = "   ", RecurrenceType.DAILY, 1, "#000000")

        assertTrue(result.isFailure)
        assertEquals(0, repository.added.size)
    }

    @Test
    fun `invoke trims the name before persisting`() = runTest {
        val repository = CapturingHabitRepository()
        val useCase = AddHabitUseCase(repository)

        useCase(name = "  Read  ", RecurrenceType.WEEKLY, 2, "#FF0000")

        assertEquals(1, repository.added.size)
        val habit = repository.added.single()
        assertEquals("Read", habit.name)
        assertEquals(RecurrenceType.WEEKLY, habit.recurrenceType)
        assertEquals(2, habit.targetCount)
        assertEquals("#FF0000", habit.color)
    }

    @Test
    fun `invoke returns success when repository succeeds`() = runTest {
        val repository = CapturingHabitRepository(addResult = Result.success(Unit))
        val useCase = AddHabitUseCase(repository)

        val result = useCase("Walk", RecurrenceType.DAILY, 1, "#00FF00")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `invoke propagates repository failure`() = runTest {
        val error = RuntimeException("firestore down")
        val repository = CapturingHabitRepository(addResult = Result.failure(error))
        val useCase = AddHabitUseCase(repository)

        val result = useCase("Walk", RecurrenceType.DAILY, 1, "#00FF00")

        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}

private class CapturingHabitRepository(
    private val addResult: Result<Unit> = Result.success(Unit)
) : HabitRepository {
    val added = mutableListOf<Habit>()

    override fun getHabits(): Flow<Result<List<Habit>>> = flowOf(Result.success(emptyList()))
    override fun getCompletionsForHabit(habitId: String): Flow<Result<List<HabitCompletion>>> =
        flowOf(Result.success(emptyList()))
    override fun getAllCompletions(): Flow<Result<List<HabitCompletion>>> =
        flowOf(Result.success(emptyList()))

    override suspend fun addHabit(habit: Habit): Result<Unit> {
        added += habit
        return addResult
    }

    override suspend fun updateHabit(habit: Habit): Result<Unit> = Result.success(Unit)
    override suspend fun deleteHabit(habitId: String): Result<Unit> = Result.success(Unit)
    override suspend fun toggleCompletion(habitId: String, date: String): Result<Unit> = Result.success(Unit)
}
