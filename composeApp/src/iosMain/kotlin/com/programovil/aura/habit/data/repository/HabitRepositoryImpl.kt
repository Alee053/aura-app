package com.programovil.aura.habit.data.repository

import com.programovil.aura.habit.data.local.HabitUserDefaults
import com.programovil.aura.habit.domain.model.Habit
import com.programovil.aura.habit.domain.model.HabitCompletion
import com.programovil.aura.habit.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

actual class HabitRepositoryImpl(
    private val userDefaults: HabitUserDefaults
) : HabitRepository {

    override fun getHabits(): Flow<List<Habit>> {
        return userDefaults.habitsFlow()
    }

    override fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletion>> {
        return userDefaults.completionsFlow().map { list ->
            list.filter { it.habitId == habitId }
        }
    }

    override fun getAllCompletions(): Flow<List<HabitCompletion>> {
        return userDefaults.completionsFlow()
    }

    override suspend fun addHabit(habit: Habit): Result<Unit> = runCatching {
        userDefaults.saveHabit(habit)
    }

    override suspend fun deleteHabit(habitId: String): Result<Unit> = runCatching {
        userDefaults.deleteHabit(habitId)
    }

    override suspend fun toggleCompletion(habitId: String, date: String): Result<Unit> = runCatching {
        userDefaults.toggleCompletion(habitId, date)
    }

    override suspend fun cleanupOldCompletions(olderThanDays: Int): Result<Unit> = runCatching {
        val cutoffDate = java.time.LocalDate.now().minusDays(olderThanDays.toLong())
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        userDefaults.deleteOldCompletions(cutoffDate)
    }
}