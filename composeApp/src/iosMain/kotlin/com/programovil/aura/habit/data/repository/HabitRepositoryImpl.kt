package com.programovil.aura.habit.domain.repository

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual fun createHabitRepository(): HabitRepository = IosHabitRepositoryImpl()

private class IosHabitRepositoryImpl : HabitRepository {
    override fun getHabits(): Flow<Result<List<Habit>>> = flowOf(Result.success(emptyList()))
    override fun getCompletionsForHabit(habitId: String): Flow<Result<List<HabitCompletion>>> = flowOf(Result.success(emptyList()))
    override fun getAllCompletions(): Flow<Result<List<HabitCompletion>>> = flowOf(Result.success(emptyList()))
    override suspend fun addHabit(habit: Habit): Result<Unit> = Result.success(Unit)
    override suspend fun deleteHabit(habitId: String): Result<Unit> = Result.success(Unit)
    override suspend fun toggleCompletion(habitId: String, date: String): Result<Unit> = Result.success(Unit)
}