package com.programovil.aura.habit.domain.repository

import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun getHabits(): Flow<Result<List<Habit>>>
    fun getCompletionsForHabit(habitId: String): Flow<Result<List<HabitCompletion>>>
    fun getAllCompletions(): Flow<Result<List<HabitCompletion>>>
    suspend fun addHabit(habit: Habit): Result<Unit>
    suspend fun deleteHabit(habitId: String): Result<Unit>
    suspend fun toggleCompletion(habitId: String, date: String): Result<Unit>
}

expect fun createHabitRepository(): HabitRepository
